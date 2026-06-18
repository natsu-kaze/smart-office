package com.natsukaze.smartoffice.fileservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.PageQuery;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.fileservice.dto.FileRecordCreateRequest;
import com.natsukaze.smartoffice.fileservice.entity.FileRecord;
import com.natsukaze.smartoffice.fileservice.config.MinioProperties;
import com.natsukaze.smartoffice.fileservice.mapper.FileRecordMapper;
import com.natsukaze.smartoffice.fileservice.vo.FileRecordVO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordMapper fileRecordMapper;

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    @Transactional
    public FileRecordVO create(Long uploaderId, FileRecordCreateRequest request) {
        FileRecord record = new FileRecord();
        record.setOriginalName(request.getOriginalName());
        record.setStorageName(request.getStorageName());
        record.setBucket(request.getBucket());
        record.setObjectKey(request.getObjectKey());
        record.setContentType(request.getContentType());
        record.setSize(request.getSize() == null ? 0L : request.getSize());
        record.setUrl(request.getUrl());
        record.setUploaderId(uploaderId);
        record.setBusinessType(BusinessType.ofNullable(request.getBusinessType()));
        record.setBusinessId(request.getBusinessId());
        fileRecordMapper.insert(record);
        return toVO(record);
    }

    @Transactional
    public FileRecordVO upload(Long uploaderId, MultipartFile file, String businessType, Long businessId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file is empty");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "unnamed";
        String storageName = UUID.randomUUID() + extension(originalName);
        String objectKey = objectKey(uploaderId, storageName);
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("upload file to minio failed");
        }

        FileRecord record = new FileRecord();
        record.setOriginalName(originalName);
        record.setStorageName(storageName);
        record.setBucket(minioProperties.getBucket());
        record.setObjectKey(objectKey);
        record.setContentType(contentType);
        record.setSize(file.getSize());
        record.setUrl("minio://" + minioProperties.getBucket() + "/" + objectKey);
        record.setUploaderId(uploaderId);
        record.setBusinessType(BusinessType.ofNullable(businessType));
        record.setBusinessId(businessId);
        fileRecordMapper.insert(record);
        return toVO(record);
    }

    public FileRecordVO detail(Long id) {
        return toVO(requireFile(id));
    }

    public PageResult<FileRecordVO> myFiles(Long uploaderId, PageQuery query) {
        Page<FileRecord> page = fileRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getUploaderId, uploaderId)
                        .orderByDesc(FileRecord::getCreateTime));
        return PageResult.from(page.convert(this::toVO));
    }

    public String previewUrl(Long id) {
        FileRecord record = requireFile(id);
        if (StringUtils.hasText(record.getUrl()) && !record.getUrl().startsWith("minio://")) {
            return record.getUrl();
        }
        try {
            ensureObjectExists(record);
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(record.getBucket())
                    .object(record.getObjectKey())
                    .expiry(minioProperties.getPreviewExpiryMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("generate file preview url failed");
        }
    }

    public FileDownload download(Long id) {
        FileRecord record = requireFile(id);
        try {
            ensureObjectExists(record);
            Resource resource = new InputStreamResource(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(record.getBucket())
                    .object(record.getObjectKey())
                    .build()));
            return new FileDownload(record.getOriginalName(), record.getContentType(), record.getSize(), resource);
        } catch (Exception ex) {
            throw new BusinessException("download file from minio failed");
        }
    }

    @Transactional
    public void delete(Long id) {
        FileRecord record = requireFile(id);
        if (StringUtils.hasText(record.getUrl()) && record.getUrl().startsWith("minio://")) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(record.getBucket())
                        .object(record.getObjectKey())
                        .build());
            } catch (Exception ex) {
                throw new BusinessException("delete file from minio failed");
            }
        }
        fileRecordMapper.deleteById(id);
    }

    private FileRecord requireFile(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("file not found");
        }
        return record;
    }

    private FileRecordVO toVO(FileRecord record) {
        return FileRecordVO.builder()
                .id(record.getId())
                .originalName(record.getOriginalName())
                .storageName(record.getStorageName())
                .bucket(record.getBucket())
                .objectKey(record.getObjectKey())
                .contentType(record.getContentType())
                .size(record.getSize())
                .url(record.getUrl())
                .uploaderId(record.getUploaderId())
                .businessType(record.getBusinessType() == null ? null : record.getBusinessType().getCode())
                .businessId(record.getBusinessId())
                .build();
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .build());
        }
    }

    private void ensureObjectExists(FileRecord record) throws Exception {
        minioClient.statObject(StatObjectArgs.builder()
                .bucket(record.getBucket())
                .object(record.getObjectKey())
                .build());
    }

    private String objectKey(Long uploaderId, String storageName) {
        LocalDate now = LocalDate.now();
        return "uploader-" + uploaderId + "/"
                + now.getYear() + "/"
                + "%02d".formatted(now.getMonthValue()) + "/"
                + "%02d".formatted(now.getDayOfMonth()) + "/"
                + storageName;
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index);
    }

    public record FileDownload(String filename, String contentType, Long size, Resource resource) {
    }
}
