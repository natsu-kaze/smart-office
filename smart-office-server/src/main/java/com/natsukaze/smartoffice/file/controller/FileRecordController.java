package com.natsukaze.smartoffice.file.controller;

import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.file.dto.FileRecordCreateRequest;
import com.natsukaze.smartoffice.file.service.FileRecordService;
import com.natsukaze.smartoffice.file.vo.FileRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileRecordController {

    private final FileRecordService fileRecordService;

    @PostMapping("/records")
    public Result<FileRecordVO> createRecord(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody FileRecordCreateRequest request) {
        return Result.success(fileRecordService.create(principal.getUserId(), request));
    }

    @GetMapping("/{id}")
    public Result<FileRecordVO> detail(@PathVariable Long id) {
        return Result.success(fileRecordService.detail(id));
    }

    @GetMapping("/{id}/preview")
    public Result<String> preview(@PathVariable Long id) {
        return Result.success(fileRecordService.previewUrl(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileRecordService.delete(id);
        return Result.success();
    }
}
