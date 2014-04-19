package edu.buct.glasearch.search.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import net.semanticmetadata.lire.indexing.parallel.ImageInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.entity.SearchResult;
import edu.buct.glasearch.search.service.ImageProcessService;

@Controller
@RequestMapping(value = "/search")
public class ImageSearchController {

	@Autowired
	private ImageProcessService imageProcessService;
	
	Gson gson = new Gson();
	
	@RequestMapping(value="ms")
	@ResponseBody
	public String mobileSearch(
			@RequestParam(required=false) MultipartFile imageFile, 
			@RequestParam(required=false) MultipartFile voiceFile, 
			@RequestParam(required=false) String lat,
			@RequestParam(required=false) String lng,
			@RequestParam(required=false) String seq) throws Exception {
		
		if (StringUtils.isEmpty(seq)) {
			seq = UUID.randomUUID().toString();
		}
		
		ImageInfo imageInfo = imageProcessService.buildImageInfo(imageFile, voiceFile, lat, lng);
		List<ImageInformation> imageResult = imageProcessService.search(imageInfo, null);
		
		SearchResult result = new SearchResult();
		result.setImageList(imageResult);
		result.setWords(imageInfo.getTitle());
		result.setLat(lat);
		result.setLng(lng);
		
		return gson.toJson(result);
	}
	
	@RequestMapping(value="s")
	public String search(
			@RequestParam MultipartFile file, 
			@RequestParam(required=false) Integer method,
			@ModelAttribute ImageInformation imageInfo, 
			Model model) throws IOException {
		
		imageInfo.setBuffer(file.getBytes());
		List<ImageInformation> imageList = imageProcessService.search(imageInfo, method);
		
		model.addAttribute("result", imageList);
		return "search/glasearch";
	}

	@RequestMapping("main")
	public String main(Model model) {
		
		return "search/glasearch";
	}
	
	@RequestMapping(value="index")
	public String index(Model model) {
		try {
			imageProcessService.indexImages();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:main";
	}
	
	@RequestMapping(value="reindex")
	public String reindex(Model model) throws IOException {
		try {
			imageProcessService.reindexImages();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:main";
	}
	
	@RequestMapping("image")
	public void image(String id, Model model, HttpServletResponse response) throws IOException {
		ImageInformation image = this.imageProcessService.load(id);
		String imagePath = imageProcessService.getImagePath() + File.separator + image.getId() + ".jpg";
		
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        
		FileInputStream fs = new FileInputStream(imagePath);
		
		IOUtils.copy(fs, response.getOutputStream());
		response.flushBuffer();
		fs.close();
	}
}
