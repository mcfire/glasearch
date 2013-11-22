package edu.buct.glasearch.search.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.docbuilder.ChainedDocumentBuilder;
import net.semanticmetadata.lire.impl.searcher.VisualWordsImageSearcher;
import net.semanticmetadata.lire.indexing.LireCustomCodec;
import net.semanticmetadata.lire.indexing.parallel.ImageInfo;
import net.semanticmetadata.lire.indexing.parallel.ParallelIndexer;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.buct.glasearch.search.entity.ImageInformation;
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
	
    int numResults = 50;
    int method = 2;
    
    public ImageProcessService() {
    }
    
    public ImageInformation load(Long id) {
    	return imageInfoDao.findOne(id);
    }
    
    public String getImagePath() {
    	return context.getRealPath("images-data");
    }
    
    public String getIndexPath() {
    	return context.getRealPath("search-index");
    }
    
    public void deleteAllIndex() throws IOException {
    	IndexWriterConfig config = new IndexWriterConfig(LuceneUtils.LUCENE_VERSION, new StandardAnalyzer(LuceneUtils.LUCENE_VERSION));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setCodec(new LireCustomCodec());
    	IndexWriter writer = new IndexWriter(FSDirectory.open(new File(getIndexPath())), config);
    	writer.deleteAll();
    	writer.close();
    }
    
    public void reindexImages() throws IOException {
    	this.deleteAllIndex();
    	this.indexImages();
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
	
	public List<ImageInformation> search(ImageInfo imageInfo, Integer method) throws IOException {
		
		if (method != null) {
			this.method = method;
		}
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(getIndexPath())));
        int numDocs = reader.numDocs();
        logger.info("numDocs = " + numDocs);

        ImageSearcher searcher = getSearcher();
        ImageSearchHits hits = searcher.search(
        		ImageIO.read(new ByteArrayInputStream(imageInfo.getBuffer())), reader);
        reader.close();
        
        List<ImageInformation> imageList = new ArrayList<ImageInformation>();
		if (hits.length() > 0) {
			for (int i = 0;i < hits.length();i++) {
				Document doc = hits.doc(i);
				float distance = hits.score(i);
				
				ImageInformation image = new ImageInformation();

				try {
					image.setId(doc.getField(DocumentBuilder.FIELD_NAME_DBID).numericValue().longValue());
					image.setTitle(doc.get(DocumentBuilder.FIELD_NAME_TITLE));
					image.setLocation(doc.get(DocumentBuilder.FIELD_NAME_LOCATION));
					image.setLng(doc.get(DocumentBuilder.FIELD_NAME_LNG));
					image.setLat(doc.get(DocumentBuilder.FIELD_NAME_LAT));
					image.setTags(doc.get(DocumentBuilder.FIELD_NAME_TAGS));
					image.setDistance(distance);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
				imageList.add(image);
			}
		}
		
        return imageList;
	}

    private ImageSearcher getSearcher() {

        ImageSearcher searcher = ImageSearcherFactory.createColorLayoutImageSearcher(numResults);
        if (method == 1) {
            searcher = ImageSearcherFactory.createScalableColorImageSearcher(numResults);
        } else if (method == 2) {
            searcher = ImageSearcherFactory.createEdgeHistogramImageSearcher(numResults);
        } else if (method == 3) {
            searcher = ImageSearcherFactory.createAutoColorCorrelogramImageSearcher(numResults);
        } else if (method == 4) { // CEDD
            searcher = ImageSearcherFactory.createCEDDImageSearcher(numResults);
        } else if (method == 5) { // FCTH
            searcher = ImageSearcherFactory.createFCTHImageSearcher(numResults);
        } else if (method == 6) { // JCD
            searcher = ImageSearcherFactory.createJCDImageSearcher(numResults);
        } else if (method == 7) { // SimpleColorHistogram
            searcher = ImageSearcherFactory.createColorHistogramImageSearcher(numResults);
        } else if (method == 8) {
            searcher = ImageSearcherFactory.createTamuraImageSearcher(numResults);
        } else if (method == 9) {
            searcher = ImageSearcherFactory.createGaborImageSearcher(numResults);
        } else if (method == 10) {
            searcher = ImageSearcherFactory.createJpegCoefficientHistogramImageSearcher(numResults);
        } else if (method == 11) {
            searcher = new VisualWordsImageSearcher(numResults, DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS);
        } else if (method == 12) {
            searcher = ImageSearcherFactory.createJointHistogramImageSearcher(numResults);
        } else if (method == 13) {
            searcher = ImageSearcherFactory.createOpponentHistogramSearcher(numResults);
        } else if (method == 14) {
            searcher = ImageSearcherFactory.createLuminanceLayoutImageSearcher(numResults);
        } else if (method >= 15) {
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
