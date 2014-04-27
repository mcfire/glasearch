package edu.buct.glasearch.search.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;

import edu.buct.glasearch.search.jobs.ImageIndexJob;
import edu.buct.glasearch.search.jobs.ImageSearchJob;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureList;
import edu.buct.glasearch.search.jobs.ImageSearchJob.Map;
import edu.buct.glasearch.search.jobs.ImageSearchJob.Reduce;

//Spring Bean的标识.
@Component
//默认将类中的所有public函数纳入事务管理.
@Transactional
public class ImageProcessJobService {
	
	private static Logger logger = LoggerFactory.getLogger(ImageProcessService.class);
	
	private static final String START_ROW = "39";
	private static final String STOP_ROW = "39-393300";
	
	@Autowired
	Configuration conf;

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
	    Job job = configureSearchJob(conf, null, null, 20000L);

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
