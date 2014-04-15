package edu.buct.glasearch.search.entity;

import java.util.List;

public class SearchResult {
	
	private String words;

    private String lat;

    private String lng;
	
	List<ImageInformation> imageList;

	public List<ImageInformation> getImageList() {
		return imageList;
	}

	public void setImageList(List<ImageInformation> imageList) {
		this.imageList = imageList;
	}

	public String getWords() {
		return words;
	}

	public void setWords(String words) {
		this.words = words;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLng() {
		return lng;
	}

	public void setLng(String lng) {
		this.lng = lng;
	}
	
}
