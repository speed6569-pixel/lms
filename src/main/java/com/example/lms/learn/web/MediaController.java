package com.example.lms.learn.web;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
public class MediaController {

    @GetMapping("/media/videos/{filename}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        Path path = Path.of("/home/ubuntu/project/lms/uploads/videos").resolve(filename).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        MediaType type = filename.endsWith(".webm") ? MediaType.valueOf("video/webm") : MediaType.valueOf("video/mp4");
        return ResponseEntity.ok().contentType(type).body(resource);
    }
}
