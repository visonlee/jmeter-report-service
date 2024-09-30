package com.lws.jmeter.service.impl;

import com.lws.jmeter.config.StorageProperties;
import com.lws.jmeter.entity.Script;
import com.lws.jmeter.exception.StorageException;
import com.lws.jmeter.exception.StorageFileNotFoundException;
import com.lws.jmeter.model.ScriptStatus;
import com.lws.jmeter.service.ScriptService;
import com.lws.jmeter.service.StorageService;
import com.lws.jmeter.util.CompressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileSystemStorageService implements StorageService {

	private static final String EXTRACTED_FOLDER_SUFFIX = "_extracted";
	private static final String REPORT_FOLDER_SUFFIX = "_report";

	private final Path rootLocation;
	private final ScriptService scriptService;

	@Autowired
	public FileSystemStorageService(StorageProperties properties, ScriptService scriptService ) {
		this.scriptService = scriptService;
        
        if(properties.getLocation().trim().isEmpty()){
            throw new StorageException("File upload location can not be Empty."); 
        }

		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}

			String fileName = file.getOriginalFilename();

			String baseName = FilenameUtils.getBaseName(fileName);
			String extension = FilenameUtils.getExtension(fileName);

			List<String> allowExtensions = List.of("zip", "ZIP", "jmx", "JMX");
			if (!allowExtensions.contains(extension)) {
				throw new StorageException("illegal file: " + fileName);
			}
			File workingDir = this.rootLocation.resolve(baseName).toFile();
			if (workingDir.exists()) {
				throw new StorageException(baseName + ": already exists");
			}else {
				boolean mkdirSuccess = workingDir.mkdir();
				if (!mkdirSuccess) {
					throw new StorageException("fail to create " + workingDir.getAbsolutePath());
				}
			}

			Path destinationFile = workingDir.toPath().resolve(Paths.get(fileName)).normalize()
					.toAbsolutePath();

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destinationFile,
						StandardCopyOption.REPLACE_EXISTING);
			}

			Path extractedFilePath = destinationFile;

		    if ("zip".equalsIgnoreCase(extension)) {
				try {
					Path extractFolder = workingDir.toPath().resolve(baseName + EXTRACTED_FOLDER_SUFFIX);
					CompressUtils.extract(destinationFile, extractFolder);
					extractedFilePath = extractScriptFromDirectory(extractFolder);
				} catch (Exception e) {
					log.error("extract failure for " + fileName, e);
					if (workingDir.exists()) {
						FileSystemUtils.deleteRecursively(workingDir);
					}
					throw new StorageException("fail to extract " + fileName, e);
				}
			}

			Script script = Script.builder()
					.userId(1L)
					.filename(fileName)
					.uploadedFullPath(destinationFile.toString())
					.extractedFullPath(extractedFilePath.toString())
					.reportDirectory(workingDir.toPath().resolve(baseName + REPORT_FOLDER_SUFFIX).toString())
					.status(ScriptStatus.NOT_STARTED)
					.build();
			try {
				scriptService.save(script);
			}catch (Exception e) {
				if (workingDir.exists()) {
					FileSystemUtils.deleteRecursively(workingDir);
				}
				throw new StorageException("fail to save script " + workingDir.getAbsolutePath(), e);
			}
		}
		catch (IOException e) {
			throw new StorageException("Failed to store file.", e);
		}
	}

	private Path extractScriptFromDirectory(Path directory) {
		try (Stream<Path> fileStream = Files.walk(directory, 2)) {
			List<Path> jmxFiles = fileStream
					.filter(path -> "jmx".equalsIgnoreCase(FilenameUtils.getExtension(path.getFileName().toString())))
					.toList();
			if (jmxFiles.isEmpty()) {
				throw new StorageException("we can not find any jmx script in your upload file");
			}

			if (jmxFiles.size() > 1) {
				throw new StorageException("you've more than 1 jmx file in the upload which currently not supported");
			}
			return jmxFiles.get(0);
		} catch (IOException e) {
			throw new StorageException("unable to find the jmx file", e);
		}
	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException(
						"Could not read file: " + filename);
			}
		}
		catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}


	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		}
		catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
