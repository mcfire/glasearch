package edu.buct.glasearch.search.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.stereotype.Component;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureList;
import edu.buct.glasearch.search.jobs.ImageSearchJob.FeatureObject;
import edu.buct.glasearch.search.repository.ImageInfoDao;

//Spring Bean的标识.
@Component
public class ImageContentSearcher extends AbstractImageSearcher {
	
	@Autowired
	private ImageProcessJobService imageProcessJobService;
	@Autowired
	private ImageInfoDao imageInfoDao;
	
	private int resultCount = 20;

	@Override
	public ImageSearchHits search(BufferedImage image, IndexReader reader)
			throws IOException {
		
		FeatureList outColorFeatureResult = new FeatureList();
		FeatureList outEdgeFeatureResult = new FeatureList();
		
		try {
			imageProcessJobService.searchImage(image, resultCount, outColorFeatureResult, outEdgeFeatureResult);
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		Map<String, FeatureObject> mergedResultMap = new HashMap<String, FeatureObject>();
		List<FeatureObject> colorResult = outColorFeatureResult.getResult();
		List<FeatureObject> edgeResult = outEdgeFeatureResult.getResult();
		if (colorResult.size() == 0 && edgeResult.size() == 0) return null;
		
		float maxDistance = 0;
		
		if (colorResult.size() > 0) {
			maxDistance = colorResult.get(colorResult.size() - 1).getDistance();
		}
		if (edgeResult.size() > 0) {
			float distance = edgeResult.get(edgeResult.size() - 1).getDistance();
			if (distance > maxDistance) maxDistance = distance;
		}
		
		for (FeatureObject feature : colorResult) {
			feature.setDistance(maxDistance - feature.getDistance());
			
			mergedResultMap.put(feature.getRowId(), feature);
		}
		
		for (FeatureObject feature : outEdgeFeatureResult.getResult()) {
			feature.setDistance(maxDistance - feature.getDistance());
			
			if (!mergedResultMap.containsKey(feature.getRowId())) {
				mergedResultMap.put(feature.getRowId(), feature);
			} else {
				FeatureObject existFeature = mergedResultMap.get(feature.getRowId());
				existFeature.setDistance(existFeature.getDistance() + feature.getDistance());
			}
		}
		List<FeatureObject> mergedResultList = new ArrayList<FeatureObject>(mergedResultMap.size());
		mergedResultList.addAll(mergedResultMap.values());
		Collections.sort(mergedResultList, Collections.reverseOrder());
		
		List<SimpleResult> results = new ArrayList<SimpleResult>();
		
		maxDistance = mergedResultList.get(0).getDistance();
		for (FeatureObject feature : mergedResultList) {
			ImageInformation imageInfo = imageInfoDao.getById(feature.getRowId());
			Document doc = new Document();
			DocumentUtils.appendImageInfoFields(doc, imageInfo);
			
			SimpleResult r = new SimpleResult(maxDistance - feature.getDistance(), doc, 0);
			results.add(r);
		}
		ImageSearchHits hits = new SimpleImageSearchHits(results, maxDistance);
		
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

	public int getResultCount() {
		return resultCount;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

}
