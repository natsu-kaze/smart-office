package com.natsukaze.smartoffice.fileservice.controller;

import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.core.PageQuery;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.fileservice.dto.FileRecordCreateRequest;
import com.natsukaze.smartoffice.fileservice.service.FileRecordService;
import com.natsukaze.smartoffice.fileservice.vo.FileRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileRecordController {

    private final FileRecordService fileRecordService;

    @GetMapping
    public Result<PageResult<FileRecordVO>> myFiles(@RequestHeader("X-User-Id") Long userId,
                                                    @ModelAttribute PageQuery query) {
        return Result.success(fileRecordService.myFiles(userId, query));
    }

    @PostMapping("/records")
    public Result<FileRecordVO> createRecord(@RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody FileRecordCreateRequest request) {
        return Result.success(fileRecordService.create(userId, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileRecordVO> upload(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String businessType,
                                       @RequestParam(required = false) Long businessId) {
        return Result.success(fileRecordService.upload(userId, file, businessType, businessId));
    }

    @GetMapping("/{id}")
    public Result<FileRecordVO> detail(@PathVariable Long id) {
        return Result.success(fileRecordService.detail(id));
    }

    @GetMapping("/{id}/preview")
    public Result<String> preview(@PathVariable Long id) {
        return Result.success(fileRecordService.previewUrl(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileRecordService.FileDownload download = fileRecordService.download(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null) {
            mediaType = MediaType.parseMediaType(download.contentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.size() == null ? -1L : download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileRecordService.delete(id);
        return Result.success();
    }
}
