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
		image.setId(new String(result.getRow()));
		image.setTitle(new String(result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.TITLE_COLUMN_BYTES)));
		image.setLat(new String(result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.LAT_COLUMN_BYTES)));
		image.setLng(new String(result.getValue(
				ImageSearchJob.COLUMN_FAMILY_BYTES, ImageSearchJob.LNG_COLUMN_BYTES)));
		return image;
	}
}
