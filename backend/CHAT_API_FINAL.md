# 채팅 시스템 API 명세서 (WebSocket 중심)

## Overview
VIMS 화상회의 시스템의 실시간 채팅 기능 API 명세서입니다.
- **입장/퇴장/메시지**: WebSocket (실시간)
- **상태 조회**: REST API (필요시만)

---

## WebSocket Connection

### 연결 설정
- **Endpoint**: `/ws`
- **Protocol**: STOMP over WebSocket
- **Fallback**: SockJS 지원
- **CORS**: 모든 Origin 허용

### 연결 예시
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    // 연결 후 바로 강의실 입장 가능
});
```

---

## 1. 강의실 채팅 (WebSocket)

### 1.1 강의실 입장 + 구독

#### WebSocket Message
- **Destination**: `/app/lecture.join`
- **Method**: SEND

#### Request Body
```json
{
    "lectureId": "lecture123",
    "userId": "user123", 
    "userName": "홍길동",
    "userRole": "STUDENT"  // "INSTRUCTOR", "STUDENT", "TA"
}
```

#### Response (자동 브로드캐스트)
- **Topic**: `/topic/lecture.{lectureId}`
```json
{
    "id": "uuid-123",
    "lectureId": "lecture123",
    "senderId": "user123",
    "senderName": "홍길동", 
    "content": "홍길동님이 강의실에 입장했습니다. (참여자 5명)",
    "type": "JOIN",
    "timestamp": "2024-01-15T10:30:00"
}
```

**💡 입장과 동시에 해당 토픽 자동 구독됨**

### 1.2 일반 채팅 메시지

#### WebSocket Message
- **Destination**: `/app/lecture.send`
- **Method**: SEND

#### Request Body
```json
{
    "lectureId": "lecture123",
    "senderId": "user123",
    "senderName": "홍길동",
    "content": "안녕하세요! 질문이 있습니다.",
    "type": "CHAT"
}
```

#### Response (브로드캐스트)
- **Topic**: `/topic/lecture.{lectureId}`
```json
{
    "id": "uuid-125",
    "lectureId": "lecture123", 
    "senderId": "user123",
    "senderName": "홍길동",
    "content": "안녕하세요! 질문이 있습니다.",
    "type": "CHAT",
    "timestamp": "2024-01-15T10:35:00"
}
```

### 1.3 공지사항 (강사/TA만)

#### WebSocket Message
- **Destination**: `/app/lecture.send`
- **Method**: SEND

#### Request Body
```json
{
    "lectureId": "lecture123",
    "senderId": "instructor1",
    "senderName": "김교수",
    "content": "잠시 후 퀴즈를 진행하겠습니다.",
    "type": "ANNOUNCEMENT"
}
```

#### Response (브로드캐스트)
- **Topic**: `/topic/lecture.{lectureId}`
```json
{
    "id": "uuid-126",
    "lectureId": "lecture123",
    "senderId": "instructor1", 
    "senderName": "김교수",
    "content": "잠시 후 퀴즈를 진행하겠습니다.",
    "type": "ANNOUNCEMENT",
    "timestamp": "2024-01-15T10:40:00"
}
```

### 1.4 강의실 퇴장

#### WebSocket Message
- **Destination**: `/app/lecture.leave`
- **Method**: SEND

#### Request Body
```json
{
    "lectureId": "lecture123",
    "userId": "user123",
    "userName": "홍길동",
    "userRole": "STUDENT"
}
```

#### Response (브로드캐스트)
- **Topic**: `/topic/lecture.{lectureId}`
```json
{
    "id": "uuid-124",
    "lectureId": "lecture123",
    "senderId": "user123",
    "senderName": "홍길동",
    "content": "홍길동님이 강의실에서 퇴장했습니다. (참여자 4명)",
    "type": "LEAVE",
    "timestamp": "2024-01-15T11:00:00"
}
```

**💡 WebSocket 연결 해제시 자동 퇴장됨**

---

## 2. 개인 DM 채팅 (WebSocket)

### 2.1 DM 메시지 전송

#### WebSocket Message
- **Destination**: `/app/dm.send`
- **Method**: SEND

#### Request Body
```json
{
    "senderId": "user123",
    "senderName": "홍길동",
    "receiverId": "user456", 
    "receiverName": "이영희",
    "content": "개인적으로 질문드리고 싶은 것이 있습니다."
}
```

#### Response
1. **DM 방 브로드캐스트**
   - **Topic**: `/topic/dm.user123_user456`
   ```json
   {
       "id": "uuid-127",
       "senderId": "user123",
       "senderName": "홍길동",
       "receiverId": "user456",
       "receiverName": "이영희", 
       "content": "개인적으로 질문드리고 싶은 것이 있습니다.",
       "type": "TEXT",
       "timestamp": "2024-01-15T10:45:00"
   }
   ```

2. **수신자 개인 알림**
   - **Topic**: `/user/user456/queue/dm-notification`
   ```json
   {
       "messageId": "uuid-127",
       "senderId": "user123", 
       "senderName": "홍길동",
       "preview": "새로운 메시지가 도착했습니다: 개인적으로 질문드리고 싶은 것이...",
       "timestamp": "2024-01-15T10:45:00"
   }
   ```

---

## 3. 상태 조회 API (REST)

### 3.1 강의실 참여자 조회

#### HTTP Request
- **Method**: `GET`
- **Path**: `/api/lectures/{lectureId}/participants`

#### Response
```json
{
    "lectureId": "lecture123",
    "participants": ["user123", "user456", "user789"],
    "participantCount": 3,
    "isActive": true
}
```

### 3.2 활성 강의실 목록

#### HTTP Request
- **Method**: `GET`
- **Path**: `/api/lectures/active`

#### Response
```json
{
    "lecture123": 5,
    "lecture456": 12,
    "lecture789": 3
}
```

### 3.3 강의실 상태 조회

#### HTTP Request
- **Method**: `GET`
- **Path**: `/api/lectures/{lectureId}/status`

#### Response
```json
{
    "lectureId": "lecture123",
    "isActive": true,
    "participantCount": 5
}
```

---

## 4. 메시지 타입 정의

### 4.1 강의실 메시지 타입
- `CHAT`: 일반 채팅 메시지
- `JOIN`: 입장 메시지 (자동 생성)
- `LEAVE`: 퇴장 메시지 (자동 생성)  
- `SYSTEM`: 시스템 메시지
- `ANNOUNCEMENT`: 공지사항 (강사/TA만)

### 4.2 DM 메시지 타입
- `TEXT`: 텍스트 메시지
- `FILE`: 파일 메시지 (향후 지원)
- `SYSTEM`: 시스템 메시지

---

## 5. 클라이언트 구현 예시

### 5.1 WebSocket 연결 및 강의실 입장
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 1. WebSocket 연결
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 2. 강의실 구독 설정
    subscribeToLecture('lecture123');
    
    // 3. 강의실 입장 (구독과 입장이 동시에)
    joinLecture('lecture123', 'user123', '홍길동', 'STUDENT');
});

// 강의실 메시지 구독
function subscribeToLecture(lectureId) {
    stompClient.subscribe('/topic/lecture.' + lectureId, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayLectureMessage(chatMessage);
    });
}

// 강의실 입장
function joinLecture(lectureId, userId, userName, userRole) {
    const joinMessage = {
        lectureId: lectureId,
        userId: userId,
        userName: userName,
        userRole: userRole
    };
    stompClient.send("/app/lecture.join", {}, JSON.stringify(joinMessage));
}

// 메시지 전송
function sendMessage(lectureId, senderId, senderName, content, isAnnouncement = false) {
    const chatMessage = {
        lectureId: lectureId,
        senderId: senderId,
        senderName: senderName,
        content: content,
        type: isAnnouncement ? "ANNOUNCEMENT" : "CHAT"
    };
    stompClient.send("/app/lecture.send", {}, JSON.stringify(chatMessage));
}

// 강의실 퇴장
function leaveLecture(lectureId, userId, userName, userRole) {
    const leaveMessage = {
        lectureId: lectureId,
        userId: userId,
        userName: userName,
        userRole: userRole
    };
    stompClient.send("/app/lecture.leave", {}, JSON.stringify(leaveMessage));
}
```

### 5.2 DM 채팅
```javascript
// DM Room ID 생성 함수
function generateDMRoomId(userId1, userId2) {
    if (userId1.localeCompare(userId2) < 0) {
        return `dm_${userId1}_${userId2}`;
    } else {
        return `dm_${userId2}_${userId1}`;
    }
}

// DM 구독
function subscribeToDM(userId1, userId2) {
    const roomId = generateDMRoomId(userId1, userId2);
    
    stompClient.subscribe('/topic/dm.' + roomId, function(message) {
        const dmMessage = JSON.parse(message.body);
        displayDirectMessage(dmMessage);
    });
    
    // 개인 알림 구독
    stompClient.subscribe('/user/queue/dm-notification', function(notification) {
        const notificationData = JSON.parse(notification.body);
        showNotification(notificationData);
    });
}

// DM 전송
function sendDirectMessage(senderId, senderName, receiverId, receiverName, content) {
    const dmMessage = {
        senderId: senderId,
        senderName: senderName,
        receiverId: receiverId,
        receiverName: receiverName,
        content: content
    };
    stompClient.send("/app/dm.send", {}, JSON.stringify(dmMessage));
}
```

### 5.3 연결 해제 (자동 퇴장)
```javascript
// 페이지 닫기 전 정리
window.addEventListener('beforeunload', function() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();  // 자동으로 강의실에서 퇴장됨
    }
});
```

---

## 6. 장점

### 6.1 WebSocket 중심 구조의 장점
- **즉시성**: 입장/퇴장이 실시간으로 반영
- **자동 정리**: 연결 끊김시 자동 퇴장 처리
- **단순함**: 클라이언트가 하나의 연결로 모든 기능 사용
- **일관성**: 모든 실시간 이벤트가 WebSocket으로 통합

### 6.2 REST API 보완
- **상태 조회**: 필요시 안정적인 HTTP 요청으로 상태 확인
- **디버깅**: HTTP 요청은 브라우저에서 쉽게 확인 가능
- **확장성**: 향후 채팅 이력, 파일 업로드 등 추가 기능

### 6.3 사용자 경험
- **자연스러움**: 실제 화상회의처럼 입장하면 바로 채팅 가능
- **안정성**: 네트워크 문제시 자동으로 상태 정리
- **직관적**: 복잡한 API 호출 순서 없이 간단한 메시지 전송

---

## 7. 보안 고려사항

- **세션 관리**: WebSocket 세션에 사용자 정보 저장
- **권한 검증**: 서버에서 공지사항 권한 자동 확인  
- **자동 정리**: 비정상 종료시에도 안전한 상태 유지
- **메시지 검증**: 필수 필드 및 권한 확인