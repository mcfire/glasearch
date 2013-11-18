package edu.buct.glasearch.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.impl.VisualWordsImageSearcher;
import net.semanticmetadata.lire.indexing.parallel.ParallelIndexer;
import net.semanticmetadata.lire.indexing.parallel.WorkItem;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import edu.buct.glasearch.service.indexing.MetadataBuilder;

public class ImageProcess {
	
	String indexPath = "d:/glassinput/image-index";
	
    int builderIdx = 1;
    int numResults = 50;

	public void indexImages() {
    	
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        
        List<WorkItem> images = new ArrayList<WorkItem>();

        ParallelIndexer pin =
                new net.semanticmetadata.lire.indexing.parallel.ParallelIndexer(
                		8, indexPath, images){
                    @Override
                    public void addBuilders(ChainedDocumentBuilder builder) {
                        builder.addBuilder(new MetadataBuilder());
                    }
                };
        Thread t = new Thread(pin);
        t.start();
	}
	
	public ImageSearchHits findImage(String imageFileName) throws IOException {
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        int numDocs = reader.numDocs();
        System.out.println("numDocs = " + numDocs);
        ImageSearcher searcher = getSearcher();
        ImageSearchHits hits = searcher.search(ImageIO.read(new FileInputStream(imageFileName)), reader);
        reader.close();
        return hits;
	}

    private ImageSearcher getSearcher() {

        ImageSearcher searcher = ImageSearcherFactory.createColorLayoutImageSearcher(numResults);
        if (builderIdx == 1) {
            searcher = ImageSearcherFactory.createScalableColorImageSearcher(numResults);
        } else if (builderIdx == 2) {
            searcher = ImageSearcherFactory.createEdgeHistogramImageSearcher(numResults);
        } else if (builderIdx == 3) {
            searcher = ImageSearcherFactory.createAutoColorCorrelogramImageSearcher(numResults);
        } else if (builderIdx == 4) { // CEDD
            searcher = ImageSearcherFactory.createCEDDImageSearcher(numResults);
        } else if (builderIdx == 5) { // FCTH
            searcher = ImageSearcherFactory.createFCTHImageSearcher(numResults);
        } else if (builderIdx == 6) { // JCD
            searcher = ImageSearcherFactory.createJCDImageSearcher(numResults);
        } else if (builderIdx == 7) { // SimpleColorHistogram
            searcher = ImageSearcherFactory.createColorHistogramImageSearcher(numResults);
        } else if (builderIdx == 8) {
            searcher = ImageSearcherFactory.createTamuraImageSearcher(numResults);
        } else if (builderIdx == 9) {
            searcher = ImageSearcherFactory.createGaborImageSearcher(numResults);
        } else if (builderIdx == 10) {
            searcher = ImageSearcherFactory.createJpegCoefficientHistogramImageSearcher(numResults);
        } else if (builderIdx == 11) {
            searcher = new VisualWordsImageSearcher(numResults, DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS);
        } else if (builderIdx == 12) {
            searcher = ImageSearcherFactory.createJointHistogramImageSearcher(numResults);
        } else if (builderIdx == 13) {
            searcher = ImageSearcherFactory.createOpponentHistogramSearcher(numResults);
        } else if (builderIdx == 14) {
            searcher = ImageSearcherFactory.createLuminanceLayoutImageSearcher(numResults);
        } else if (builderIdx >= 15) {
            searcher = ImageSearcherFactory.createPHOGImageSearcher(numResults);
        }
        return searcher;
    }
	
	public void bagOfWordsIndex() throws IOException {
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        int samples = Math.max(1000, reader.numDocs() / 2);
        final SurfFeatureHistogramBuilder builder = new SurfFeatureHistogramBuilder(reader, samples, 500);
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    builder.index();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
	}
}
