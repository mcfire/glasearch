package edu.buct.glasearch.search.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.semanticmetadata.lire.ImageSearchHits;

import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.service.ImageProcessService;

@Controller
@RequestMapping(value = "/search")
public class ImageSearchController {

	@Autowired
	private ImageProcessService imageProcessService;

	@RequestMapping("main")
	public String main(Model model) {
		
		return "search/main";
	}
	
	@RequestMapping(value="index")
	public String index(Model model) {
		imageProcessService.indexImages();
		return "search/main";
	}
	
	@RequestMapping(value="s")
	public String search(
			@RequestParam MultipartFile file, 
			@ModelAttribute ImageInformation imageInfo, 
			Model model) throws IOException {
		
		imageInfo.setBuffer(file.getBytes());
		ImageSearchHits result = imageProcessService.search(imageInfo);
		
		List<ImageInformation> imageList = new ArrayList<ImageInformation>();
		if (result.length() > 0) {
			for (int i = 0;i < result.length();i++) {
				Document doc = result.doc(i);
				
				ImageInformation image = new ImageInformation();
				image.setTitle(doc.get("title"));
				image.setLocation(doc.get("location"));
				image.setTags(doc.get("tags"));
				
				imageList.add(image);
			}
		}
		
		model.addAttribute("result", imageList);
		return "search/main";
	}
}
