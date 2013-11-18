package edu.buct.glasearch.search.web;

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.buct.glasearch.search.service.ImageProcessService;

@Controller
@RequestMapping(value = "/search")
public class ImageSearchController {

	@Autowired
	private ImageProcessService imageProcessService;

	@RequestMapping()
	public String main(Model model, ServletRequest request) {
		
		return "search/main";
	}
	
	@RequestMapping(value="index")
	public String index(Model model, ServletRequest request) {
		imageProcessService.indexImages();
		return "search/main";
	}
}
