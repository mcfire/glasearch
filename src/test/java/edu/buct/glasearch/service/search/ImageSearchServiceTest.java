package edu.buct.glasearch.service.search;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springside.modules.test.security.shiro.ShiroTestUtils;

import edu.buct.glasearch.search.service.ImageProcessService;
import edu.buct.glasearch.user.service.account.ShiroDbRealm.ShiroUser;

public class ImageSearchServiceTest {

	@Autowired
	private ImageProcessService imageProcessService;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ShiroTestUtils.mockSubject(new ShiroUser("3", "foo", "Foo"));
	}

	@Test
	public void voiceTest() throws Exception {
		
		String text = imageProcessService.voiceToText(getClass().getResource("/search/data1.raw").openStream());
		assertNotNull(text);
	}


}
