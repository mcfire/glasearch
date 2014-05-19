package edu.buct.glasearch.search.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CompatibilityFactory;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.JobUtil;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.protobuf.generated.HBaseProtos;
import org.apache.hadoop.hbase.thrift2.ThriftServer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.vint.UVLongTool;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import scala.Tuple2;

import com.google.gson.Gson;
import com.google.protobuf.HBaseZeroCopyByteString;
import com.google.protobuf.LazyStringArrayList;

import edu.buct.glasearch.search.jobs.ImageIndexJob;
import edu.buct.glasearch.search.jobs.ImageSearchJob;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureList;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureObject;
import edu.buct.glasearch.search.jobs.ImageSearchJob.Map;
import edu.buct.glasearch.search.jobs.ImageSearchJob.Reduce;
import edu.buct.glasearch.search.jobs.SparkImageSearchJob;

//Spring Bean的标识.
@Component
//默认将类中的所有public函数纳入事务管理.
@Transactional
public class ImageProcessJobService implements Serializable {
	
	private static Logger logger = LoggerFactory.getLogger(ImageProcessService.class);

	Configuration conf;

	double colorAvg, colorSigma, edgeAvg, edgeSigma;
	
	JavaSparkContext ctx;
	
	@Autowired
	public ImageProcessJobService(Configuration conf) {
		this.conf = conf;
		
		try {
			this.sparkSetup();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sparkSetup() throws IOException {
		
		Set<String> jars = new HashSet<String>();

		addToList(JavaSparkContext.jarOfClass(SparkImageSearchJob.class), jars);
		addToList(JavaSparkContext.jarOfClass(SimpleColorHistogram.class), jars);

		addToList(JavaSparkContext.jarOfClass(StringUtils.class), jars);
		addToList(JavaSparkContext.jarOfClass(LogFactory.class), jars);
		addToList(JavaSparkContext.jarOfClass(Configuration.class), jars);
		addToList(JavaSparkContext.jarOfClass(HTable.class), jars);
		addToList(JavaSparkContext.jarOfClass(TableInputFormat.class), jars);
		addToList(JavaSparkContext.jarOfClass(HBaseConfiguration.class), jars);
		addToList(JavaSparkContext.jarOfClass(HBaseProtos.class), jars);
		addToList(JavaSparkContext.jarOfClass(JobUtil.class), jars);
		addToList(JavaSparkContext.jarOfClass(CompatibilityFactory.class), jars);
		addToList(JavaSparkContext.jarOfClass(UVLongTool.class), jars);
		addToList(JavaSparkContext.jarOfClass(ThriftServer.class), jars);
		addToList(JavaSparkContext.jarOfClass(org.cloudera.htrace.Trace.class), jars);
		
		addToList(JavaSparkContext.jarOfClass(HBaseZeroCopyByteString.class), jars);
		addToList(JavaSparkContext.jarOfClass(LazyStringArrayList.class), jars);
		
		String masterAddress = conf.get("spark.address");
		ctx = new JavaSparkContext(
				masterAddress, 
				"SparkSearchJob",
				System.getenv("SPARK_HOME"),
				jars.toArray(new String[jars.size()]));
		
		//从距离信息表中提取图像间距离的平均值和方差，用于距离的归一化。
		HTable distanceTable = new HTable(conf, ImageSearchJob.imageDistanceTable);
		Get colorDistanceGet = new Get(ImageSearchJob.COLOR_FEATURE_RESULT_COLUMN);
		Result colorDistance = distanceTable.get(colorDistanceGet);
		colorAvg = Bytes.toDouble(colorDistance.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, Bytes.toBytes("avg")));
		colorSigma = Bytes.toDouble(colorDistance.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, Bytes.toBytes("sigma")));
		
		Get edgeDistanceGet = new Get(ImageSearchJob.EDGE_FEATURE_RESULT_COLUMN);
		Result edgeDistance = distanceTable.get(edgeDistanceGet);
		edgeAvg = Bytes.toDouble(edgeDistance.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, Bytes.toBytes("avg")));
		edgeSigma = Bytes.toDouble(edgeDistance.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, Bytes.toBytes("sigma")));
		
		distanceTable.close();
		
	}
	
	private void addToList(String[] array, Set<String> list) {
		if (array == null || list == null) return;
		for (String element : array) {
			list.add(element);
		}
	}

	public void searchBySpark(BufferedImage image, int resultSize,
			FeatureList outColorFeatureResult, FeatureList outEdgeFeatureResult) throws IOException, InterruptedException {		
		
		Scan scan = new Scan();
		scan.setCaching(500);        // 1 is the default in Scan, which will be bad for MapReduce jobs
		scan.setCacheBlocks(false);  // don't set to true for MR jobs
		
		conf.set(TableInputFormat.INPUT_TABLE, ImageSearchJob.imageInfoTable);
		// read data
		JavaPairRDD<ImmutableBytesWritable, Result> hbaseData = ctx.newAPIHadoopRDD(conf, 
				TableInputFormat.class, 
				ImmutableBytesWritable.class, Result.class);
		
		LireFeature colorFeature = new SimpleColorHistogram();
		colorFeature.extract(image);
		LireFeature edgeFeature = new EdgeHistogram();
		edgeFeature.extract(image);
		
		SparkImageSearchJob.Map map = new SparkImageSearchJob.Map();
		map.setup(colorFeature, edgeFeature, colorAvg, colorSigma, edgeAvg, edgeSigma, conf);
		
		JavaPairRDD<Double, FeatureObject> colorRdd = hbaseData.flatMap(map);
		JavaPairRDD<Double, FeatureObject> edgeRdd = colorRdd.cache();
		
		List<Tuple2<Double, FeatureObject>> colorResult = colorRdd.
				filter(new SparkImageSearchJob.FeatureFilter(FeatureObject.FeatureType.color)).
				sortByKey(true).take(resultSize);
		
		List<Tuple2<Double, FeatureObject>> edgeResult = edgeRdd.
				filter(new SparkImageSearchJob.FeatureFilter(FeatureObject.FeatureType.edge)).
				sortByKey(true).take(resultSize);

		List<FeatureObject> colorResultList = new ArrayList<FeatureObject>();
		outColorFeatureResult.setResult(colorResultList);
		for (Tuple2<Double, FeatureObject> t : colorResult) {
			colorResultList.add(t._2);
		}
		List<FeatureObject> edgeResultList = new ArrayList<FeatureObject>();
		outEdgeFeatureResult.setResult(edgeResultList);
		for (Tuple2<Double, FeatureObject> t : edgeResult) {
			edgeResultList.add(t._2);
		}
	}

	/**
	 * Job configuration.
	 */
	private Job configureIndexJob(Configuration conf)
			throws IOException {

		//利用此工具方法将相关jar包加入Hadoop引用，否则需手工引用
		TableMapReduceUtil.addDependencyJars(conf, ImageIndexJob.class, LireFeature.class);
		
		//设置任务名称
		JobConf jobConf = new JobConf(conf);
		jobConf.setJobName("image-index");
		
		//创建Hadoop的MapReduce任务
		Job job = new Job(jobConf);
		job.setJarByClass(ImageIndexJob.class);
		
		Scan scan = new Scan();
		scan.setCaching(500);
		scan.setCacheBlocks(false);
		
		//利用HBase提供的工具方法初始化任务对象
		TableMapReduceUtil.initTableMapperJob(
				ImageSearchJob.imageInfoTable,  //输入数据所在的表
				scan,               			//用于读取数据表的扫描对象
				ImageIndexJob.class,     		//Map操作所在的类
				null,         					//Map操作的输出Key类型
				null,  							//Map操作的输出Value类型
				job);
		
		TableMapReduceUtil.initTableReducerJob(
				ImageSearchJob.imageInfoTable,      //数据输出表
				null,             					//Reduce操作所在的类
				job);
		job.setNumReduceTasks(0);					//设置为没有Reduce类
		return job;
	}

	public void indeImages() throws Exception {
	    
		Job job = configureIndexJob(conf);

		job.waitForCompletion(true);
	}

	/**
	 * Job configuration.
	 */
	private Job configureSearchJob(Configuration conf, byte[] startRow, byte[] stopRow, Long maxResultSize)
			throws IOException {

		//利用此工具方法将相关jar包加入Hadoop引用，否则需手工引用
		TableMapReduceUtil.addDependencyJars(conf, ImageSearchJob.class, LireFeature.class, Gson.class);
		
		//设置任务名称
		JobConf jobConf = new JobConf(conf);
		jobConf.setJobName("image-search");
		
		Job job = new Job(jobConf);
		job.setJarByClass(ImageIndexJob.class);
		
		Scan scan = new Scan();
		scan.setCaching(500);        // 1 is the default in Scan, which will be bad for MapReduce jobs
		scan.setCacheBlocks(false);  // don't set to true for MR jobs
		scan.setStartRow(startRow);
		scan.setStopRow(stopRow);
		
		if (maxResultSize != null && maxResultSize > 0) {
			Filter pageFilter = new PageFilter(maxResultSize);
			scan.setFilter(pageFilter);
		}
		
		// set other scan attrs
		
		//利用HBase提供的工具方法初始化任务对象
		TableMapReduceUtil.initTableMapperJob(
				ImageSearchJob.imageInfoTable,	//输入数据所在的表
				scan,               			//用于读取数据表的扫描对象
				Map.class,     					//Map操作所在的类
				Text.class,         			//Map操作的输出Key类型
				Text.class,  					//Map操作的输出Value类型
				job);
		
		TableMapReduceUtil.initTableReducerJob(
				ImageSearchJob.imageResultTable,//数据输出表
				Reduce.class,             		//Reduce操作所在的类
				job);
		job.setNumReduceTasks(1);				//设置为拥有一个Reduce类
		return job;
	}

	public void searchImage(BufferedImage image, int resultSize,
			FeatureList outColorFeatureResult, FeatureList outEdgeFeatureResult) throws Exception {
		
		//提取待检索图像的颜色直翻图和边缘直方图特征
		LireFeature colorFeature = new SimpleColorHistogram();
		colorFeature.extract(image);
		LireFeature edgeFeature = new EdgeHistogram();
		edgeFeature.extract(image);

		//生成检索信息表的rowId
		String rowId = UUID.randomUUID().toString();

		//将待检索图像特征存入图像信息表
	    HTable table = new HTable(conf, ImageSearchJob.imageResultTable);
	    Put featuresPut = new Put(rowId.getBytes());
	    featuresPut.add(ImageSearchJob.COLUMN_FAMILY_BYTES, 
	    		ImageSearchJob.COLOR_FEATURE_COLUMN, colorFeature.getByteArrayRepresentation());
	    featuresPut.add(ImageSearchJob.COLUMN_FAMILY_BYTES, 
	    		ImageSearchJob.EDGE_FEATURE_COLUMN, edgeFeature.getByteArrayRepresentation());
	    table.put(featuresPut);
	    
	    conf.set(ImageSearchJob.SEARCH_ROWID, rowId);
	    conf.setInt("resultSize", resultSize);
	    //配置检索任务
		//Job job = configureSearchJob(conf, Bytes.toBytes(START_ROW),  Bytes.toBytes(STOP_ROW));
	    Job job = configureSearchJob(conf, null, null, null);

		//执行检索任务
		boolean isSuccess = job.waitForCompletion(true);
		
		if (isSuccess) {
			//若检索成功，则提取检索结果
			Gson gson = new Gson();
			
			//根据rowId提取检索结果
			Get featuresGet = new Get(rowId.getBytes());
			Result result = table.get(featuresGet);
			byte[] colorFeatureResultBytes = result.getValue(
					ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.COLOR_FEATURE_RESULT_COLUMN);
			String colorFeatureResultJson = new String(colorFeatureResultBytes);
			//将检索结果由JSON格式转换为对象列表格式
			FeatureList colorFeatureResult = gson.fromJson(colorFeatureResultJson, FeatureList.class);
			outColorFeatureResult.setResult(colorFeatureResult.getResult());
			
			byte[] edgeFeatureResultBytes = result.getValue(
					ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.EDGE_FEATURE_RESULT_COLUMN);
			String edgeFeatureResultJson = new String(edgeFeatureResultBytes);
			//将检索结果由JSON格式转换为对象列表格式
			FeatureList edgeFeatureResult = gson.fromJson(edgeFeatureResultJson, FeatureList.class);
			outEdgeFeatureResult.setResult(edgeFeatureResult.getResult());
		}
		
	    table.close();
	}
}
