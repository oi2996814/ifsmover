# IfsMover

## 개요

### 용도
* 기존 NAS나 S3 호환 Object Storage에 있는 파일 및 오브젝트를 InfiniStor 또는 KSAN으로 이관


### 주요 기능
* NAS(SMB/NFS) / Local 파일시스템의 파일을 InfiniStor 또는 KSAN으로 이관 지원
* AWS S3의 오브젝트를 InfiniStor 또는 KSAN으로 이관 지원
* NAS(SMB/NFS) / Local 파일시스템의 파일을 AWS S3로 이관 지원
* openstack Swift의 오브젝트를 InfiniStor 또는 KSAN으로 이관 지원 - Version 3 Authentication 지원
* 재수행 옵션(-rerun)을 통해 소스 경로의 신규 및 수정된 오브젝트 및 파일만 추가로 이관하는 기능 제공
* 상태 확인 옵션(-status)을 통해 현재 수행 중인 이관 작업의 상태를 실시간으로 모니터링 가능 
* 수행 쓰레드 수 지정 옵션(-thread=`number`)을 통해 이관 작업 부하 및 성능 제어 가능
* (주의) Versioning을 지원하지 않습니다. 최신 파일만 이관합니다. Source에서 ListObjects한 대상만 Target으로 이관합니다.

### 실행 옵션
```sh
ifs_mover -t=s3|file|swift –source=source.conf -target=target.conf -o=ea,perm,time -thread=10 
Usage : ifs_mover [OPTION] ...
Move Objects
        -t=file|s3|swift        source type, FILE(NAS) or S3 or SWIFT
        -source=source.conf     source configuration file path
        -target=target.conf     target configuration file path
        -o=ea,perm,time         object meta info
                ea               save fils's extented attribute in S3 meta
                perm             save file's permission(rwxrwxrwx) in S3 meta
                                 744, READ permission granted to AUTHENTICATED_USER and PUBLIC
                time     save file's C/M/A time in S3 meta
        -thread=                thread count
Stop Job
        -jobstop=jobid          stop a job in progress
Remove Job
        -jobremove=jobid        delete stopped job information
Rerun
        -rerun=jobid            function to execute only the DELTA part
                                by performing it again based on the previously
                                performed JOB information
        -source=source.conf     source configuration file path
        -target=target.conf     target configuration file path
        -thread=                thread count
Check
        -check                  check source and target configuration
        -t=file|s3|swift        source type, FILE(NAS) or S3 or SWIFT
        -source=source.conf     source configuration file path
        -target=target.conf     target configuration file path
Status Job
        -status                 show all jobs progress
        -jobId=jobid            show job progress for jobid
        -srcbucket=bucket       show job progress for srcbucket
        -dstbucket=bucket       show job progress for dstbucket
source.conf
        mountpoint              information mounted on the server to be performed
                                mountpoint=/ means move all files
        endpoint                http(https)://IP:Port | region
        access                  Access Key ID
        secret                  Secret Access Key
        bucket                  bucket name
        prefix                  PREFIX DIR name from which to start the MOVE
        part_size               Part size if multipart is used. (M bytes)
        use_multipart           Use multipart if it is larger than that size. (G/M default M)
        metadata                Set whether to include metadata. on/off (default on)
        tag                     Set tag to include tag info. on/off (default on)
        acl                     object ACL on, off (default off)
        // use for swift
        user_name               user name for swift
        api_key                 api key for swift
        auth_endpoint           http(https)://IP:port/v3 authentication endpoint for swift
        domain_id               domain id for swift
        domain_name             domain name for swift
        project_id              project id for swift
        project_name            project name for swift
        container               list of containers(If it is empty, it means all container)
target.conf
        endpoint                http(https)://IP:Port | region
        access                  Access Key ID
        secret                  Secret Access Key
        bucket                  bucket name
        prefix                  PREFIX DIR name from which to start the MOVED
        sync                    target object sync on, off
        sync_mode               target object sync mode, [etag|size|exist]
        acl                     object ACL on, off (default off)
주) –o는 향후 개발 예정
```

## 실행 예시(CLI-Linux)
### 설정 파일 체크
```sh
ifs_mover -check -t=file -source=source.conf -target=target.conf
or
ifs_mover -check -t=s3 -source=source.conf -target=target.conf
or
ifs_mover -check -t=swift -source=source.conf -target=target.conf
```

### AWS S3 -> InfiniStor/KSAN 이관 작업 등록 및 실행
```sh
ifs_mover -t=s3 -source=source.conf -target=target.conf -thread=4
```

### NAS -> InfiniStor/KSAN 이관 작업 등록 및 실행
```sh
ifs_mover -t=file -source=source.conf -target=target.conf -thread=4
```

### Job ID가 1로 배정된 이관 작업을 재수행 
```sh
ifs_mover -rerun=1 -source=source.conf -target=target.conf -thread=4
```

! 기존 이관 완료된 데이터를 제외하고 이관 작업이 수행됨

! 재수행하는 대상 이관 작업(Job ID가 동일한 작업)이 이미 수행 중인 상태라면 해당 작업을 중지하고 다시 시작하는 방식으로 구현됨


### 전체 이관 작업의 상태정보 조회
```sh
ifs_mover -status               // 모든 Job에 대한 작업 상태 정보 조회
ifs_mover -status -jobid=3      // jobId = 3인 Job에 대한 작업 상태 정보 조회
ifs_mover -status -srcbucket=bucket-1   // source bucket 이름에 "bucket-1" 포함된 Job에 대한 작업 상태 정보 조회
ifs_mover -status -dstbucket=bucket-1   // target bucket 이름에 "bucket-1" 포함된 Job에 대한 작업 상태 정보 조회
ifs_mover -status -srcbucket=bucket-1 -dstbucket=bucket-2   // source bucket 이름에 "bucket-1" 포함되고, target bucket 이름에 "bucket-2"가 포함된 Job에 대한 작업 상태 정보 조회
```


### Job ID가 1로 배정된 이관 작업을 중지
```sh
ifs_mover -jobstop=1
```

### Job ID가 1로 배정된 이관 작업을 삭제
```sh
ifs_mover -jobremove=1
```

## 설정 파일
### source.conf
```sh
source.conf 
    mountpoint:     mountPoint
    endpoint:       http(https)://IP:Port | region
    access:         Access Key ID
    secret:         Secret Access Key
    bucket:         Bucket Name
    prefix:         MOVE를 시작할 PREFIX/DIR 이름 정보
    part_size:      Part size if multipart is used. (M bytes)
    use_multipart   Use multipart if it is larger than that size. (G/M, default M)
    metadata        Set whether to include metadata. on/off (default on)
    tag             Set tag to include tag info. on/off (default on)
    acl:            object ACL 정보 획득 여부(on/off)

    // use for swift
    user_name       user name for swift
    api_key         api key for swift
    auth_endpoint   http(https)://IP:port/v3
    domain_id       domain id for swift
    domain_name     domain name for swift
    project_id      project id for swift
    project_name    project name for swift
    container       list of containers (If it is empty, it means all container)
```

* endpoint : protocol(http|https):// IP:Port | region (AWS)

### target.conf
```sh
target.conf
    endpoint:   http(https)://IP:Port | region
    access:     Access Key ID
    secret:     Secret Access Key
    bucket:     Bucket Name
    prefix:     저장 될 PREFIX/DIR 이름 정보
    sync:       on으로 지정하면 sync_mode에 따라 target object를 검사하여 같은 경우 skip 한다.
    sync_mode:  [etag|size|exist] 
                etag : target에 source object가 존재하고 etag가 같은 경우 skip
                size : target에 source object가 존재하고 size가 같은 경우 skip
                exist : target에 source object가 존재하는 경우 skip
                sync=on 이고, sync_mode 값이 없는 경우 etag가 기본 값으로 설정된다.
                * 주의 - type=file인 경우에는 etag로 지정하여도 etag를 검사하지 않는다. type=file 인 경우 source 파일의 etag를 수집하지 않음.
    acl:        object ACL 복사 여부(on/off)
```

### 설정 파일 예시

#### file(NAS) -> S3 (/mnt/volume01/move_old_objects/* -> /move-test/*)
```sh
source.conf 
    mountpoint=/mnt/volume01/
    endpoint=
    access=
    secret=
    bucket=
    prefix=move_old_objects
    part_size=
    use_multipart=

target.conf
    endpoint=http://192.168.11.02:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=
```

#### file(NAS) -> S3 (AWS) (/mnt/volume01/move_old_objects/* -> /move-test/2021_07_20/*)
```sh
source.conf 
    mountpoint=/mnt/volume01/
    endpoint=
    access=
    secret=
    bucket=
    prefix=move_old_objects
    part_size=
    use_multipart=

target.conf
    endpoint=ap-northeast-2
    access=AHDFSDLKJD98KDA55QFQ
    secret=AdkjJDKDSDjksdkTBEFjgUIZav0kFG/
    bucket=move-test
    prefix=2021_07_20
```

#### S3 -> S3 (move-test/move_old_objects/* -> /move-test/*)
```sh
source.conf 
    mountpoint=
    endpoint=http://www.s3abc.com:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=move_old_objects
    part_size=
    use_multipart=

target.conf
    endpoint=https://www.s3other.com:8443
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=
```

#### S3 -> S3 (move-test/move_old_objects/* -> /move-test/*) + ACL 정보도 같이 복사하려는 경우
```sh
source.conf 
    mountpoint=
    endpoint=http://www.s3abc.com:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=move_old_objects
    part_size=
    use_multipart=
    acl=on

target.conf
    endpoint=https://www.s3other.com:8443
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=
    acl=on
```

#### S3 -> S3 (AWS) (/move-test/0720/* -> /move-test/*)
```sh
source.conf 
    mountpoint=
    endpoint=http://www.s3abc.com:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=0720
    part_size=
    use_multipart=

target.conf
    endpoint=ap-northeast-2
    access=AHDFSDLKJD98KDA55QFQ
    secret=AdkjJDKDSDjksdkTBEFjgUIZav0kFG
    bucket=move-test
    prefix=
```

#### S3(AWS) -> S3 (/move-test/move_old_objects/* -> /move-test/*)
```sh
source.conf 
    mountpoint=
    endpoint=ap-northeast-2
    access=AHDFSDLKJD98KDA55QFQ
    secret=AdkjJDKDSDjksdkTBEFjgUIZav0kFG
    bucket=move-test
    prefix=move_old_objects
    part_size=
    use_multipart=

target.conf
    endpoint=http://192.168.11.02:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=
```

#### S3(AWS) -> S3(AWS) (/old_objects/* -> /move-test/*)
```sh
source.conf 
    mountpoint=
    endpoint=ap-northeast-2
    access=AHDFSDLKJD98KDA55QFQ
    secret=AdkjJDKDSDjksdkTBEFjgUIZav0kFG
    bucket=old_objects
    prefix=
    part_size=
    use_multipart=

target.conf
    endpoint=us-west-1
    access=DSDISDSDLKJD98KDA55QFQ
    secret=BdsDsSDsdDSDjksdkTBEFjgUIZav0kFG
    bucket=move-test
    prefix=
```

#### SWIFT -> S3 (사용자의 test_container/*, test-big-size/* -> /test-container/*, test-big-size/*)
```sh
source.conf
mountpoint=
endpoint=
access=
secret=
bucket=
prefix=
part_size=
use_multipart=
user_name=admin
api_key=9c7d08adb7414a8a
auth_endpoint=http://192.168.13.188:5000/v3
domain_id=default
domain_name=
project_id=9327b0988cab4d5c84e297345a4f3c67
project_name=
container=test_container,test-big-size

target.conf
endpoint=http://192.168.13.21:8080
access=DSDISDSDLKJD98KDA55QFQ
secret=BdsDsSDsdDSDjksdkTBEFjgUIZav0kFG
bucket=
prefix=
```

#### SWIFT -> S3 (사용자의 모든 containers -> /container1, contatiner2, ...)
```sh
source.conf
mountpoint=
endpoint=
access=
secret=
bucket=
prefix=
part_size=
use_multipart=
user_name=admin
api_key=9c7d08adb7414a8a
auth_endpoint=http://192.168.13.188:5000/v3
domain_id=default
domain_name=
project_id=9327b0988cab4d5c84e297345a4f3c67
project_name=
container=

target.conf
endpoint=http://192.168.13.21:8080
access=DSDISDSDLKJD98KDA55QFQ
secret=BdsDsSDsdDSDjksdkTBEFjgUIZav0kFG
bucket=
prefix=
```

### 실행 예시
#### Job1 - NAS to InfiniStor/KSAN
* NAS 마운트 포인트(/OSDDISK2/mydata/)에서 InfiniStor(ifs-mover-test)로 전체 데이터 마이그레이션 수행
![](/images/sample1.png)

#### Job2 - InfiniStor to InfiniStor/KSAN
* InfiniStor(ifs-mover-test)에서 InfiniStor(ifs-mover-test2)로 전체 데이터 마이그레이션 수행
![](/images/sample2.png)

#### Job3 - InfiniStor to InfiniStor/KSAN
* InfiniStor(ifs-mover-test2)에서 InfiniStor(ifs-mover-test-version/version)로 전체 데이터 마이그레이션 수행 (prefix:version)
![](/images/sample3.png)

#### RERUN Job1 - NAS to InfiniStor/KSAN (RERUN)
* NAS 마운트 포인트(/OSDDISK2/mydata/)에서 InfiniStor(ifs-mover-test)로 추가된 데이터만 마이그레이션 수행
![](/images/sample4.png)

#### Job4 - AWS S3 to InfiniStor/KSAN (Versioning)
* AWS S3(heoks-mover-test-versioning)에서 InfiniStor(ifs-mover-test-version-from-s3)로 전체 데이터 마이그레이션 수행 (Versioning 포함)
![](/images/sample5.png)
![](/images/sample6.png)

#### Job5 - Swift to InfiniStor/KSAN
* Swift(test_container)에서 InfiniStor(test-container)로 전체 데이터 마이그레이션 수행
![](/images/sample7.png)
![](/images/sample8.png)

## Windows 실행 예시
* python이 설치되어 있어야 합니다.
* <kbd>python --version</dbd> 로 설치되어 있는지 확인하세요.

* 네트워크 드라이브를 T: 로 설정한 경우
![](/images/network_driver.png)

#### file(NAS) -> S3 (T:/source_data/* -> /move-test/*)
```sh
source.conf 
    mountpoint=T:/source_data
    endpoint=
    access=
    secret=
    bucket=
    prefix=
    part_size=10        // multipart를 사용하는 경우 part 크기는 10M
    use_multipart=1G    // 1G 이상의 파일을 multipart로 전송

target.conf
    endpoint=http://192.168.11.02:8080
    access=a9dad4ce7233sdfesdfsd
    secret=sdfsdfsdfcd408e83e23dab92
    bucket=move-test
    prefix=
```

## VM에서 실행하는 경우
* source가 S3인 경우 source.conf/part_size, use_multipart의 값을 설정해 주어야 합니다.
* VM에서 ifsmover를 이용하여 파일을 옮길 때, 파일 크기가 큰 경우 JVM의 메모리가 부족하여 실패할 수 있습니다. (로그가 남지 않음)
```sh
source.conf 
    mountpoint=T:/source_data
    endpoint=
    access=
    secret=
    bucket=
    prefix=
    part_size=10         // multipart를 사용하는 경우 part 크기는 10M
    use_multipart=100    // 100M 이상의 파일을 multipart로 전송
```
* VM의 메모리 상황에 맞추어 part_size의 값을 조정해야합니다.


#### 설정 파일 체크
```sh
python ifs_mover -check -t=file -source=source.conf -target=target.conf -thread=4
```
#### 실행
```sh
python ifs_mover -t=file -source=source.conf -target=target.conf -thread=4
```

#### 주의사항
* Windows에서는 ifs_mover 앞에 python을 붙여주어야 합니다.
* swift의 objects를 옮기는 경우, target.conf에 bucket을 지정하지 않습니다. container의 이름을 사용합니다.
* swift의 container가 S3 bucket naming rule에 맞지 않는 경우에는 자동으로 bucket 명을 변경합니다.

#### swift container -> S3 bucket 이름 규칙
* container 이름을 소문자로 변경합니다.
* container 이름 길이가 3보다 작은 경우 'ifs-' 를 앞에 붙입니다.
* container 이름 길이가 63보다 큰 경우 63자까지만 bucket 이름으로 사용합니다.
* '.' or '-' 로 container 이름이 시작하는 경우 '.' or '-' 문자를 버립니다.
* '.' or '-' 로 container 이름이 끝나는 경우 '.' or '-' 문자를 버립니다.
* 'xn--'으로 container 이름이 시작하는 경우 'xn--' 문자열을 버립니다.
* '-s3alias'으로 container 이름이 끝나는 경우 '-s3alias' 문자열을 버립니다.
* '_'(under score)가  container 이름에 포함된 경우 '-'(dash)로 변경합니다.
* '..' or '.-' or '-.'이 container 이름에 포함된 경우 '..' or '.-' or '-.' 문자열을 버립니다.
* 위의 규칙으로도 S3 bucket 이름에 규칙에 맞지 않는 경우 동작하지 않습니다.

## DB Schema
![](/images/heoks_ifs_mover_db_5.png)

## conf 파일
* 위치 etc/ifs-mover.conf
```sh
db_repository=mariadb   // [sqlite | mariadb]
db_host=127.0.0.1       // mariadb 시 host ip
db_name=ifsmover        // mariadb 시 database name
db_port=3306            // mariadb 시 port
db_user=root            // mariadb 시 user name
db_password=1234        // mariadb 시 user password
db_pool_size=10         // mariadb 시 db connection pool size
replace_chars=[]+$|(){}^ // replace characters with '-'
set_targfet_path_to_lowercase=1 // change target name to lowercase
```


## 로그 파일

* 위치
  * /var/log/infinistor/mover/ifs_mover.`jobid`.log (rpm 배포 버전)
* 로그 파일 최대 크기 : 100MB
* 로그 파일 최대 유지기간 : 7일

## Dependencies

* com.amazonaws : aws-java-sdk-s3 : 1.11.256
* ch.qos.logback : logback-classic : 1.2.3
* org.slf4j : slf4j-api : ${slf4j.version}
* org.slf4j : slf4j-simple : ${slf4j.version}
* commons-cli : commons-cli : 1.4
* org.xerial : sqlite-jdbc : 3.34.0
* org.junit.jupiter : junit-jupiter-engine : 5.6.2
* commons-io : commons-io : 2.11.0
* javax.xml.bind : jaxb-api : 2.3.0
* com.github.openstack4j.core : openstack4j : 3.3
* com.googlecode.json-simple : json-simple : 1.1.1
* org.mariadb.jdbc : mariadb-java-client : 3.0.4
* com.zaxxer : HikariCP : 3.4.5

## 구동 환경

* OS : CentOS Linux release 7.5 이상
* JDK : 1.8 이상
* Python : 2.0 이상
* mariadb : 10.5.21 이상

## How to Get Started
<kbd>git clone https://github.com/infinistor/ifsmover.git</kbd>

## How to Build

### Maven 설치
* Maven이 설치되어 있는지 확인해야 합니다.

* <kbd>mvn -v</kbd> 로 설치되어 있는지 확인하세요.

* 설치가 되어 있지 않으면 다음 명령어로 설치를 해야 합니다. <br> 
<kbd>sudo apt install maven</kbd>

### Build

* pom.xml 파일이 있는 위치에서 <kbd>mvn package</kbd> 명령어를 입력하시면 빌드가 되고, 빌드가 완료되면 target이라는 폴더에 ifs-mover.jar가 생성됩니다.


## How to Use (빌드한 경우)

* IfsMover를 실행시키기 위하여 필요한 파일은 4개입니다.
 * target/ifs-mover.jar - 소스 빌드 후, 생성된 실행 파일	
 * script/ifs_mover - ifs-mover.jar를 실행시켜주는 스크립트
 * script/ifs-mover.conf - db 관련 설정
 * script/ifs-mover.xml - log파일 관련 설정

* 3개의 파일을 실행시킬 위치에 복사합니다.
 * target/ifs-mover.jar -> 실행시킬 위치/lib/ifs-mover.jar
 * script/ifs-mover.xml -> 실행시킬 위치/etc/ifs-mover.xml
 * script/ifs-mover.conf -> 실행시킬 위치/etc/ifs-mover.conf
 * script/ifs_mover -> 실행시킬 위치/ifs_mover

* ifs_mover의 실행 권한을 확인합니다.
 * ifs_mover의 실행 권한이 없는 경우 실행권한을 부여합니다. <br>
 <kbd>chmod +x ifs_mover</kbd>
 
* source.conf, target.conf에 이관 작업의 설정 정보를 입력합니다.

```bash
# vi source.conf
# vi target.conf
```

* -check 옵션으로 source.conf, target.conf가 유효한지 검사합니다. <br>
<kbd>./ifs_mover -check -t=s3 -source=source.conf -target=target.conf</kbd>

* ifs_mover를 실행합니다. <br>
<kbd>./ifs_mover -t=s3 -source=source.conf -target=target.conf</kbd>

* 더 자세한 실행 방법은 본 문서의 "실행 예시", "설정 파일 예시"를 참조하세요.

## How to Use (배포판의 경우)

* 아래 배포판 페이지의 "Asset" 항목을 펼쳐서 IfsMover-x.x.x.tar.gz 파일 링크를 미리 복사하세요.
  * 배포판 페이지 : https://github.com/infinistor/ifsmover/releases

* 배포판을 다운로드하고 압축을 풀어 설치합니다.

```bash
# mkdir /usr/local/pspace
# mkdir /usr/local/pspace/bin
# cd /usr/local/pspace/bin
# wget "https://github.com/infinistor/ifsmover/releases/download/v0.x.x/ifsmover-0.x.x.tar.gz"
# tar -xvf ifsmover-0.x.x.tar.gz
# mv ifsmover-0.x.x ifsmover
```

* 설치 경로로 이동합니다. <br>
<kbd> cd /usr/local/pspace/bin/ifsmover </kbd>

* source.conf, target.conf에 이관 작업의 설정 정보를 입력합니다.

```bash
# vi source.conf
# vi target.conf
```

* -check 옵션으로 source.conf, target.conf가 유효한지 검사합니다. <br>
<kbd>./ifs_mover -check -t=s3 -source=source.conf -target=target.conf</kbd>

* ifs_mover를 실행합니다. <br>
<kbd>./ifs_mover -t=s3 -source=source.conf -target=target.conf</kbd>

* 더 자세한 실행 방법은 본 문서의 "실행 예시", "설정 파일 예시"를 참조하세요.


