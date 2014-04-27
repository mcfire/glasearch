package edu.buct.glasearch.search.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.imageanalysis.bovw.SurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.impl.docbuilder.TextDocumentBuilder;
import net.semanticmetadata.lire.impl.searcher.MSERImageSearcher;
import net.semanticmetadata.lire.impl.searcher.SIFTImageSearcher;
import net.semanticmetadata.lire.impl.searcher.SURFImageSearcher;
import net.semanticmetadata.lire.indexing.LireCustomCodec;
import net.semanticmetadata.lire.indexing.parallel.ImageInfo;
import net.semanticmetadata.lire.indexing.parallel.ParallelIndexer;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.iflytek.speech.RecognizerListener;
import com.iflytek.speech.RecognizerResult;
import com.iflytek.speech.SpeechConfig.RATE;
import com.iflytek.speech.SpeechError;
import com.iflytek.speech.SpeechRecognizer;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.repository.ImageInfoDao;
import edu.buct.glasearch.search.service.indexing.MetadataBuilder;

//Spring Bean的标识.
@Component
//默认将类中的所有public函数纳入事务管理.
@Transactional
public class ImageProcessService {
	
	private static Logger logger = LoggerFactory.getLogger(ImageProcessJobService.class);
	
	private static final String imagePath = "/root/development/data/";
	
	@Autowired
	private ImageInfoDao imageInfoDao;
	@Autowired
	private ImageProcessJobService imageProcessJobService;
	@Autowired
	private ImageContentSearcher imageContentSearcher;
	
	@Autowired
	ServletContext context;
	
	boolean recognizeFinished = false;
	
    int numResults = 20;
    int method = 2;
    
    public ImageProcessService() {
    }
    
    public ImageInfo buildImageInfo(MultipartFile imageFile, MultipartFile voiceFile, 
    								String lat, String lng) throws IOException, Exception {
    	ImageInformation imageInfo = new ImageInformation();
    	
    	if (imageFile != null) {
    		imageInfo.setBuffer(imageFile.getBytes());
    	}
    	
    	if (voiceFile != null) {
	    	String keywords = this.voiceToText(voiceFile.getInputStream());
	    	imageInfo.setTitle(keywords);
	    	imageInfo.setTags(keywords);
	    	imageInfo.setLocation(keywords);
    	}
    	
    	imageInfo.setLat(lat);
    	imageInfo.setLng(lng);
    	
    	return imageInfo;
    }

	//FIXME change to event listener model
    public String voiceToText(InputStream voiceData) throws Exception {

    	//是否已经完成语音识别
    	recognizeFinished = false;
    	
    	//构建讯飞语音接口的参数
    	final StringBuilder sb = new StringBuilder();
    	SpeechRecognizer recognizer = SpeechRecognizer.createRecognizer("appid=52a1404c");
		recognizer.setSampleRate(RATE.rate8k);
		//调用语音识别接口
		recognizer.recognizeAudio(new RecognizerListener() {

			@Override
			public void onResults(ArrayList result, boolean arg1) {
				//读取文本格式的语音识别结果
				Iterator itor = result.iterator();
				while (itor.hasNext()) {
					RecognizerResult r = (RecognizerResult)itor.next();
					sb.append(r.text).append(" ");
				}
				recognizeFinished = true;
			}
			@Override
			public void onBeginOfSpeech() {}
			@Override
			public void onCancel() {
				recognizeFinished = true;
			}
			@Override
			public void onEnd(SpeechError arg0) {
				recognizeFinished = true;
			}
			@Override
			public void onEndOfSpeech() {}
			@Override
			public void onVolumeChanged(int arg0) {}				
		}, IOUtils.toByteArray(voiceData), "sms", "asr_ptt=0", "");

		//持续等待语音识别完成
		while (!recognizeFinished) {
			Thread.sleep(100);
		}
		//返回语音识别结果
		return sb.length() == 0 ? null : sb.toString();
	}
	
	@Transactional(readOnly = false)
	public void storeSearchFile(MultipartFile uploadFile, String seq) throws IllegalStateException, IOException {
		
		File fileStoreDir = new File("d:/upload/glass/");
		if (!fileStoreDir.isDirectory()) {
			fileStoreDir.mkdirs();
		}
		
		String filePath = fileStoreDir + "/" + seq;
		if (uploadFile.getOriginalFilename().endsWith(".raw")) {
			filePath += ".raw";
		} else if (uploadFile.getOriginalFilename().endsWith(".jpg")) {
			filePath += ".jpg";
		}
		
        File dest = new File(filePath);
        if (dest.exists()) {
        	throw new IllegalArgumentException("entity id already exist");
        }
        
        uploadFile.transferTo(dest);
	}
    
    public ImageInformation load(String id) {
    	return imageInfoDao.getById(id);
    }
    
    public String getImagePath() {
    	return imagePath;
    }
    
    public String getIndexPath() {
    	return context.getRealPath("search-index");
    }
    
    public void deleteAllIndex() throws IOException {
    	IndexWriterConfig config = new IndexWriterConfig(LuceneUtils.LUCENE_VERSION, new IKAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setCodec(new LireCustomCodec());
    	IndexWriter writer = new IndexWriter(FSDirectory.open(new File(getIndexPath())), config);
    	writer.deleteAll();
    	writer.close();
    }
    
    public void reindexImages() throws Exception {
    	this.deleteAllIndex();
    	this.indexImages();
    }

	public void indexImages() throws Exception {
    	
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        
        Iterable<ImageInfo> imageInfoList = (Iterable)imageInfoDao.getAll();

        ParallelIndexer pin =
                new net.semanticmetadata.lire.indexing.parallel.ParallelIndexer(
                		8, getImagePath(), getIndexPath(), imageInfoList.iterator()){
                    @Override
                    public void addBuilders(TextDocumentBuilder builder) {
                        builder.addBuilder(new MetadataBuilder());
                    }
                };
        Thread t = new Thread(pin);
        t.start();
        
        imageProcessJobService.indeImages();;
	}
	
	public List<ImageInformation> search(ImageInfo imageInfo, Integer method) throws IOException {
		
		if (method != null) {
			this.method = method;
		}
		
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(getIndexPath())));
        int numDocs = reader.numDocs();
        logger.info("numDocs = " + numDocs);

        ImageSearcher searcher = getSearcher(imageInfo);
        ImageSearchHits hits = searcher.search(
        		ImageIO.read(new ByteArrayInputStream(imageInfo.getBuffer())), imageInfo, reader);
        reader.close();
        
        List<ImageInformation> imageList = new ArrayList<ImageInformation>();
		if (hits.length() > 0) {
			for (int i = 0;i < hits.length();i++) {
				Document doc = hits.doc(i);
				float distance = hits.score(i);
				
				ImageInformation image = new ImageInformation();

				try {
					image.setId(doc.get(DocumentBuilder.FIELD_NAME_DBID));
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

    private ImageSearcher getSearcher(ImageInfo imageInfo) {

        ImageSearcher searcher = ImageSearcherFactory.createColorLayoutImageSearcher(numResults);
        if (method == 0) {
        	int singleSeacherResultCount = numResults * 2;
        	List<ImageSearcher> searchers = new ArrayList<ImageSearcher>();
        	
//        	ImageSearcher edgeHistogramSearcher = ImageSearcherFactory.
//					createEdgeHistogramImageSearcher(numResults);
//        	
//        	ImageSearcher colorHistogramSearcher = ImageSearcherFactory.
//        			createColorHistogramImageSearcher(numResults);
//        	colorHistogramSearcher.setWeight(0.6f);
        	
        	imageContentSearcher.setResultCount(singleSeacherResultCount);

			ImageSearcher keyWordsSearcher = ImageSearcherFactory.
					createKeyWordsSearcher(singleSeacherResultCount);
			keyWordsSearcher.setWeight(0.7f);
			
			ImageSearcher locationBasedSearcher = ImageSearcherFactory.
					createLocationBasedSearcher(singleSeacherResultCount);
			locationBasedSearcher.setWeight(0.7f);

			if (imageInfo.getBuffer() != null && imageInfo.getBuffer().length > 0) {
				searchers.add(imageContentSearcher);
			}
			//searchers.add(edgeHistogramSearcher);
			//searchers.add(colorHistogramSearcher);
			if (!StringUtils.isEmpty(imageInfo.getTitle())) {
				searchers.add(keyWordsSearcher);
			}
			if (!StringUtils.isEmpty(imageInfo.getLat()) 
				&& !StringUtils.isEmpty(imageInfo.getLng())) {
				searchers.add(locationBasedSearcher);
			}
			
            searcher = ImageSearcherFactory.createMultipleVoterImageSearcher(numResults, searchers);
            
        } else if (method == 1) {
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
            searcher = new SIFTImageSearcher(numResults, null, DocumentBuilder.FIELD_NAME_SIFT);
        } else if (method == 12) {
            searcher = new SURFImageSearcher(numResults, null, DocumentBuilder.FIELD_NAME_SURF);
        } else if (method == 13) {
            searcher = new MSERImageSearcher(numResults, null, DocumentBuilder.FIELD_NAME_MSER);
        } else if (method == 14) {
            searcher = ImageSearcherFactory.createJointHistogramImageSearcher(numResults);
        } else if (method == 15) {
            searcher = ImageSearcherFactory.createOpponentHistogramSearcher(numResults);
        } else if (method == 16) {
            searcher = ImageSearcherFactory.createLuminanceLayoutImageSearcher(numResults);
        } else if (method == 17) {
            searcher = ImageSearcherFactory.createPHOGImageSearcher(numResults);
        } else if (method == 18) {
            searcher = ImageSearcherFactory.createKeyWordsSearcher(numResults);
        } else if (method == 19) {
            searcher = ImageSearcherFactory.createLocationBasedSearcher(numResults);
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
