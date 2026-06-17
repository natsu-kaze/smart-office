package com.natsukaze.smartoffice.fileservice.service;

import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.fileservice.dto.FileRecordCreateRequest;
import com.natsukaze.smartoffice.fileservice.entity.FileRecord;
import com.natsukaze.smartoffice.fileservice.mapper.FileRecordMapper;
import com.natsukaze.smartoffice.fileservice.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordMapper fileRecordMapper;

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

    public FileRecordVO detail(Long id) {
        return toVO(requireFile(id));
    }

    public String previewUrl(Long id) {
        FileRecord record = requireFile(id);
        return record.getUrl();
    }

    @Transactional
    public void delete(Long id) {
        requireFile(id);
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
}
