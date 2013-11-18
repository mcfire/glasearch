package edu.buct.glasearch.search.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;

import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
		List<ImageInformation> imageList = imageProcessService.search(imageInfo);
		
		model.addAttribute("result", imageList);
		return "search/main";
	}
	
	@RequestMapping("image")
	@ResponseBody
	public byte[] image(Long id, Model model, HttpServletResponse response) throws IOException {
		ImageInformation image = this.imageProcessService.load(id);
		String imagePath = imageProcessService.getImagePath() + File.separator + image.getFileName();
		
		File imageFile = new File(imagePath);
		byte[] bytes = new byte[(int)imageFile.length()];
		FileInputStream fs = new FileInputStream(imagePath);
		fs.read(bytes);
		fs.close();
		
		response.setContentType("image/jpeg");
        return bytes;
	}
}
