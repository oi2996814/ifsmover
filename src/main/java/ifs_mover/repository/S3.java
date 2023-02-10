/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* ifsmover is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package ifs_mover.repository;

import java.io.InputStream;
import java.util.List;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Tag;

public interface S3 {
    void setVersioning();
    void setBucketVersioning(String status);
    String startMultipart(String bucket, String key, ObjectMetadata objectMetadata);
    String uploadPart(String bucket, String key, String uploadId, InputStream is, int partNumber, long partSize);
    // String completeMultipart(String bucket, String key, String uploadId, List<PartETag> list);
    CompleteMultipartUploadResult completeMultipart(String bucket, String key, String uploadId, List<PartETag> list);
    void setTagging(String bucket, String key, String versionId, List<Tag> tagSet);
    void setAcl(String bucket, String key, String versionId, AccessControlList acl);
    // String putObject(boolean isFile, String bucket, String key, ObjectData data, long size);
    // String putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata);
    PutObjectResult putObject(boolean isFile, String bucket, String key, ObjectData data, long size);
    PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata);

    void deleteObject(String bucket, String key, String versionId);
}

