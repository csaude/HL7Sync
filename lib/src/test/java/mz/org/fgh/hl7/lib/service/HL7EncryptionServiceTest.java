package mz.org.fgh.hl7.lib.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import mz.org.csaude.hl7.lib.service.HL7EncryptionService;
import mz.org.csaude.hl7.lib.service.HL7EncryptionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HL7EncryptionServiceTest {

	private HL7EncryptionService hl7EncryptionService;

	private String hl7FolderName;

	private String hl7FileName;

	private Path hl7FilePath;

	private String passPhrase;

	private Path tempDir; //Used for Windows testing

	public HL7EncryptionServiceTest() {
		this.hl7EncryptionService = new HL7EncryptionServiceImpl();
//		this.hl7FolderName = "/tmp/";
//		this.hl7FileName = "Patient_Demographic_Data";
		byte[] bytes = new byte[10];
		new Random().nextBytes(bytes);
		this.passPhrase = new String(bytes);
	}

	@BeforeEach
	public void beforeEach() throws IOException {
//		hl7FilePath = Paths.get(hl7FolderName, hl7FileName + ".hl7.enc");
		tempDir = Files.createTempDirectory("hl7_test");
		hl7FilePath = tempDir.resolve("Patient_Demographic_Data.hl7.enc");
	}

	@AfterEach
	public void afterEach() {
		try {
			// Clean up test files
			if (hl7FilePath != null && Files.exists(hl7FilePath)) {
				Files.delete(hl7FilePath);
			}
			if (tempDir != null && Files.exists(tempDir)) {
				Files.delete(tempDir);
			}
		} catch (IOException e) {
			System.err.println("Error during cleanup: " + e.getMessage());
		}

		// Delete all .hl7 files - Linux
//		File hl7Folder = Paths.get(hl7FolderName).toFile();
//		for (File f : hl7Folder.listFiles()) {
//			if (f.getName().endsWith(".hl7.enc")) {
//				f.delete();
//			}
//		}
	}

	@Test
	public void testEncrypt() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		hl7EncryptionService.encrypt(outputStream, passPhrase, hl7FilePath);

		assertThat(Files.exists(hl7FilePath)).isTrue();
	}

	@Test
	public void testDecrypt() throws Exception {
		// First encrypt a file
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		hl7EncryptionService.encrypt(outputStream, passPhrase, hl7FilePath);

		InputStream decryptedInputStream = hl7EncryptionService.decrypt(hl7FilePath, passPhrase);
		assertThat(decryptedInputStream != null).isTrue();

		// Cleanup the stream
		decryptedInputStream.close();
	}
}
