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
	
	@Autowired
	Configuration conf;

	/**
	 * Job configuration.
	 */
	private Job configureIndexJob(Configuration conf)
			throws IOException {

		TableMapReduceUtil.addDependencyJars(conf, ImageIndexJob.class, LireFeature.class);
		
		JobConf jobConf = new JobConf(conf);
		jobConf.setJobName("image-index");
		
		Job job = new Job(jobConf);
		job.setJarByClass(ImageIndexJob.class);
		
		Scan scan = new Scan();
		scan.setCaching(500);        // 1 is the default in Scan, which will be bad for MapReduce jobs
		scan.setCacheBlocks(false);  // don't set to true for MR jobs
		// set other scan attrs
		
		TableMapReduceUtil.initTableMapperJob(
				ImageSearchJob.imageInfoTable,        // input table
				scan,               // Scan instance to control CF and attribute selection
				ImageIndexJob.class,     // mapper class
				null,         // mapper output key
				null,  // mapper output value
				job);
		
		TableMapReduceUtil.initTableReducerJob(
				ImageSearchJob.imageInfoTable,      // output table
				null,             // reducer class
				job);
		job.setNumReduceTasks(0);
		return job;
	}

	public void indeImages() throws Exception {
	    
		Job job = configureIndexJob(conf);

		job.waitForCompletion(true);
	}

	/**
	 * Job configuration.
	 */
	private Job configureSearchJob(Configuration conf)
			throws IOException {

		//important: use this method to add job and it's dependency jar
		TableMapReduceUtil.addDependencyJars(conf, ImageSearchJob.class, LireFeature.class, Gson.class);
		
		JobConf jobConf = new JobConf(conf);
		jobConf.setJobName("image-search");
		
		Job job = new Job(jobConf);
		job.setJarByClass(ImageIndexJob.class);
		
		Scan scan = new Scan();
		scan.setCaching(500);        // 1 is the default in Scan, which will be bad for MapReduce jobs
		scan.setCacheBlocks(false);  // don't set to true for MR jobs
		// set other scan attrs
		
		TableMapReduceUtil.initTableMapperJob(
				ImageSearchJob.imageInfoTable,        // input table
				scan,               // Scan instance to control CF and attribute selection
				Map.class,     // mapper class
				Text.class,         // mapper output key
				Text.class,  // mapper output value
				job);
		
		TableMapReduceUtil.initTableReducerJob(
				ImageSearchJob.imageResultTable,      // output table
				Reduce.class,             // reducer class
				job);
		job.setNumReduceTasks(1);
		return job;
	}

	public void searchImage(BufferedImage image, 
			FeatureList outColorFeatureResult, FeatureList outEdgeFeatureResult) throws Exception {
		
		LireFeature colorFeature = new SimpleColorHistogram();
		colorFeature.extract(image);
		LireFeature edgeFeature = new EdgeHistogram();
		edgeFeature.extract(image);

		String rowId = UUID.randomUUID().toString();

	    HTable table = new HTable(conf, ImageSearchJob.imageResultTable);
	    Put featuresPut = new Put(rowId.getBytes());
	    featuresPut.add(ImageSearchJob.COLUMN_FAMILY_BYTES, 
	    		ImageSearchJob.COLOR_FEATURE_COLUMN, colorFeature.getByteArrayRepresentation());
	    featuresPut.add(ImageSearchJob.COLUMN_FAMILY_BYTES, 
	    		ImageSearchJob.EDGE_FEATURE_COLUMN, edgeFeature.getByteArrayRepresentation());
	    table.put(featuresPut);
	    
	    conf.set(ImageSearchJob.SEARCH_ROWID, rowId);
	    
		Job job = configureSearchJob(conf);

		boolean isSuccess = job.waitForCompletion(true);
		
		if (isSuccess) {
			Gson gson = new Gson();
			
			Get featuresGet = new Get(rowId.getBytes());
			Result result = table.get(featuresGet);
			byte[] colorFeatureResultBytes = result.getValue(
					ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.COLOR_FEATURE_RESULT_COLUMN);
			String colorFeatureResultJson = new String(colorFeatureResultBytes);
			FeatureList colorFeatureResult = gson.fromJson(colorFeatureResultJson, FeatureList.class);
			outColorFeatureResult.setResult(colorFeatureResult.getResult());
			
			byte[] edgeFeatureResultBytes = result.getValue(
					ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.EDGE_FEATURE_RESULT_COLUMN);
			String edgeFeatureResultJson = new String(edgeFeatureResultBytes);
			FeatureList edgeFeatureResult = gson.fromJson(edgeFeatureResultJson, FeatureList.class);
			outEdgeFeatureResult.setResult(edgeFeatureResult.getResult());
		}
		
	    table.close();
	}
}
