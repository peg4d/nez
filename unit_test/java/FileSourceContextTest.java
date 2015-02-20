import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;


public class FileSourceContextTest {
	@Test
	public void test() throws IOException, URISyntaxException {
		assertTrue(true);
		Path path = Paths.get(this.getClass().getResource("lines.txt").toURI());
		List<String> contents = Files.readAllLines(path);
		assertEquals(9999, contents.size());
	}

}
