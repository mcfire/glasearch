package edu.buct.glasearch.search.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.semanticmetadata.lire.AbstractImageSearcher;
import net.semanticmetadata.lire.ImageDuplicates;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.impl.SimpleImageSearchHits;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.indexing.parallel.ImageInfo;
import net.semanticmetadata.lire.utils.DocumentUtils;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.springframework.beans.factory.annotation.Autowired;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureList;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureObject;
import edu.buct.glasearch.search.repository.ImageInfoDao;

public class ImageContentSearcher extends AbstractImageSearcher {
	
	@Autowired
	private ImageProcessJobService imageProcessJobService;
	@Autowired
	private ImageInfoDao imageInfoDao;

	@Override
	public ImageSearchHits search(BufferedImage image, IndexReader reader)
			throws IOException {
		
		FeatureList outColorFeatureResult = new FeatureList();
		FeatureList outEdgeFeatureResult = new FeatureList();
		
		try {
			imageProcessJobService.searchImage(image, outColorFeatureResult, outEdgeFeatureResult);
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		List<SimpleResult> results = new ArrayList<SimpleResult>(outEdgeFeatureResult.getResult().size());
		for (FeatureObject feature : outEdgeFeatureResult.getResult()) {
			
			ImageInformation imageInfo = imageInfoDao.getById(feature.getRowId());
			Document doc = new Document();
			DocumentUtils.appendImageInfoFields(doc, imageInfo);
			
			SimpleResult r = new SimpleResult(feature.getDistance(), doc, 0);
			results.add(r);
		}
		ImageSearchHits hits = new SimpleImageSearchHits(results, 0);
		
		return hits;
	}

	@Override
	public ImageSearchHits search(BufferedImage image, ImageInfo imageInfo,
			IndexReader reader) throws IOException {
		return search(image, null);
	}

	@Override
	public ImageSearchHits search(Document doc, IndexReader reader)
			throws IOException {
		return null;
	}

	@Override
	public ImageDuplicates findDuplicates(IndexReader reader)
			throws IOException {
		return null;
	}

}
