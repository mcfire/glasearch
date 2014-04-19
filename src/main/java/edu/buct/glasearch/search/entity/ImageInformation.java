package edu.buct.glasearch.search.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;


@Entity
@Table(name = "image_info")
public class ImageInformation implements net.semanticmetadata.lire.indexing.parallel.ImageInfo {
	
	private String id;
	
	private String title;
	
	private String location;
	
	private String lat;
	
	private String lng;
	
	private String tags;
	
	private String fileName;
	
	private float distance;
	
	private byte[] buffer;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
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

	@Transient
	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	@Transient
	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
