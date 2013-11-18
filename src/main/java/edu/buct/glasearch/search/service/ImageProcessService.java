package edu.buct.glasearch.search.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.impl.VisualWordsImageSearcher;
import net.semanticmetadata.lire.indexing.parallel.ImageInfo;
import net.semanticmetadata.lire.indexing.parallel.ParallelIndexer;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.buct.glasearch.search.repository.ImageInformationDao;
import edu.buct.glasearch.search.service.indexing.MetadataBuilder;

//Spring Bean的标识.
@Component
//默认将类中的所有public函数纳入事务管理.
@Transactional
public class ImageProcessService {
	
	private static Logger logger = LoggerFactory.getLogger(ImageProcessService.class);
	
	@Autowired
	private ImageInformationDao imageInfoDao;
	
	@Autowired
	ServletContext context;
	
    int builderIdx = 1;
    int numResults = 50;
    
    public ImageProcessService() {
    }
    
    public String getImagePath() {
    	return context.getRealPath("images-data");
    }
    
    public String getIndexPath() {
    	return context.getRealPath("search-index");
    }

	public void indexImages() {
    	
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        
        Iterable<ImageInfo> imageInfoList = (Iterable)imageInfoDao.findAll();

        ParallelIndexer pin =
                new net.semanticmetadata.lire.indexing.parallel.ParallelIndexer(
                		8, getImagePath(), getIndexPath(), imageInfoList.iterator()){
                    @Override
                    public void addBuilders(ChainedDocumentBuilder builder) {
                        builder.addBuilder(new MetadataBuilder());
                    }
                };
        Thread t = new Thread(pin);
        t.start();
	}
	
	public ImageSearchHits search(ImageInfo imageInfo) throws IOException {
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(getIndexPath())));
        int numDocs = reader.numDocs();
        logger.info("numDocs = " + numDocs);

        ImageSearcher searcher = getSearcher();
        ImageSearchHits hits = searcher.search(
        		ImageIO.read(new ByteArrayInputStream(imageInfo.getBuffer())), reader);
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
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(getIndexPath())));
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
