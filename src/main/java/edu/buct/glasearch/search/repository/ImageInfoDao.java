package edu.buct.glasearch.search.repository;

import java.util.List;

import org.apache.hadoop.hbase.client.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.stereotype.Component;

import edu.buct.glasearch.search.entity.ImageInformation;
import edu.buct.glasearch.search.jobs.ImageSearchJob;

@Component
public class ImageInfoDao {
	
	@Autowired
	HbaseTemplate template;
	
	public ImageInformation getById(String rowId) {
		return template.get(ImageSearchJob.imageInfoTable, 
				rowId, new RowMapper<ImageInformation>() {
			@Override
			public ImageInformation mapRow(Result result, int rowNum)
					throws Exception {
				return rowToImage(result);
			}
		});
	}
	
	public List<ImageInformation> getAll() {
		return template.find(ImageSearchJob.imageInfoTable, 
				ImageSearchJob.COLUMN_FAMILY, new RowMapper<ImageInformation>() {
			@Override
			public ImageInformation mapRow(Result result, int rowNum)
					throws Exception {
				return rowToImage(result);
			}
		});
	}

	private ImageInformation rowToImage(Result result) {
		ImageInformation image = new ImageInformation();
		byte[] rowIdBytes = result.getRow();
		if (rowIdBytes == null) return null;
		
		image.setId(new String(rowIdBytes));
		
		byte[] titleBytes = result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.TITLE_COLUMN_BYTES);
		if (titleBytes != null) {
			image.setTitle(new String(titleBytes));
		}
		byte[] fileNameBytes = result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.FILENAME_COLUMN_BYTES);
		if (fileNameBytes != null) {
			image.setFileName(new String(fileNameBytes));
		}
		
		byte[] latBytes = result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.LAT_COLUMN_BYTES);
		if (latBytes != null) {
			image.setLat(new String(latBytes));
		}
		byte[] lngBytes = result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.LNG_COLUMN_BYTES);
		if (lngBytes != null) {
			image.setLng(new String(lngBytes));
		}
		return image;
	}
}
