package edu.buct.glasearch.search.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import edu.buct.glasearch.user.entity.IdEntity;


@Entity
@Table(name = "image_info")
public class ImageInformation extends IdEntity implements net.semanticmetadata.lire.indexing.parallel.ImageInfo {
	
	private String title;
	
	private String location;
	
	private String lat;
	
	private String lng;
	
	private String tags;
	
	private String fileName;
	
	@Transient
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

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
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
}
