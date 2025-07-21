# VIMS 채팅 시스템 API 명세서

## 📡 WebSocket 연결
- **Endpoint**: `/ws`
- **Protocol**: SockJS + STOMP
- **연결 방법**: SockJS → STOMP over WebSocket

## 🏫 강의실 채팅 (Lecture Chat)

### WebSocket 메시지

#### 1. 강의실 입장
- **Destination**: `/app/lecture.join`
- **Method**: `SEND`
- **Payload**:
```json
{
  "lectureId": "1",
  "userId": "123", 
  "userName": "김철수",
  "userRole": "STUDENT|INSTRUCTOR|TA"
}
```
- **필수 구독**: 
  - `/topic/lecture.{lectureId}` - 강의실 전체 메시지 수신
  - `/user/queue/lecture-history` - 개인별 히스토리 수신 (입장 직후 자동 전송)
- **자동 처리**: 입장과 동시에 최근 50개 메시지 히스토리 자동 전송

#### 2. 강의실 퇴장  
- **Destination**: `/app/lecture.leave`
- **Method**: `SEND`
- **Payload**:
```json
{
  "lectureId": "1",
  "userId": "123",
  "userName": "김철수"
}
```

#### 3. 강의실 메시지 전송
- **Destination**: `/app/lecture.send`
- **Method**: `SEND`
- **Payload**:
```json
{
  "lectureId": "1",
  "senderId": "123",
  "senderName": "김철수", 
  "content": "안녕하세요!",
  "type": "CHAT|ANNOUNCEMENT"
}
```
- **권한**: `ANNOUNCEMENT` 타입은 `INSTRUCTOR|TA` 역할만 전송 가능

#### 4. 강의실 메시지 수신
- **구독**: `/topic/lecture.{lectureId}`
- **실시간 메시지 형태**:
```json
{
  "id": "uuid",
  "lectureId": "1", 
  "senderId": "123",
  "senderName": "김철수",
  "content": "메시지 내용",
  "type": "CHAT|JOIN|LEAVE|SYSTEM|ANNOUNCEMENT",
  "timestamp": "2024-01-01T10:00:00"
}
```

#### 5. 강의실 히스토리 수신
- **구독**: `/user/queue/lecture-history` 
- **자동 전송**: 강의실 입장 직후 자동으로 전송됨
- **히스토리 형태**:
```json
[
  {
    "id": 1,
    "roomId": 49,
    "senderId": 123,
    "messageType": "CHAT|ANNOUNCEMENT|SYSTEM",
    "content": "메시지 내용",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```
- **메시지 순서 보장**: 히스토리 로딩 중 수신된 실시간 메시지는 큐에 저장 후 순차 처리

### REST API

#### 1. 강의실 채팅 히스토리 조회 (선택적 사용)
- **URL**: `GET /api/chat/lecture/history`
- **용도**: 추가 페이징이 필요한 경우만 사용 (기본 히스토리는 WebSocket으로 자동 전송)
- **Parameters**:
  - `lectureId` (required): 강의실 ID
  - `page` (optional, default: 0): 페이지 번호  
  - `size` (optional, default: 50): 페이지 크기
- **Response**:
```json
[
  {
    "id": 1,
    "roomId": 49,
    "senderId": 123,
    "messageType": "CHAT|ANNOUNCEMENT|SYSTEM", 
    "content": "메시지 내용",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```
- **주의**: 입장 직후 기본 히스토리는 WebSocket으로 자동 제공되므로 추가 페이지만 필요시 사용

#### 2. 강의실 메시지 개수 조회
- **URL**: `GET /api/chat/lecture/message-count`
- **Parameters**:
  - `lectureId` (required): 강의실 ID
- **Response**: 
```json
25
```

## 💬 다이렉트 메시지 (Direct Message)

### WebSocket 메시지

#### 1. DM 전송
- **Destination**: `/app/dm.send`
- **Method**: `SEND`
- **Payload**:
```json
{
  "senderId": "123",
  "senderName": "김철수",
  "receiverId": "456",
  "receiverName": "이영희",
  "content": "안녕하세요!"
}
```

#### 2. DM 방 입장 (히스토리 로드)
- **Destination**: `/app/dm.join`
- **Method**: `SEND`
- **Payload**:
```json
{
  "userId1": "123",
  "userId2": "456",
  "requesterId": "123"
}
```
- **구독**: `/user/queue/dm-history` - DM 히스토리 수신

#### 3. DM 메시지 수신
- **구독**: `/topic/dm.{roomId}` (roomId = `dm_{smallerId}_{largerId}`)
- **개인 알림**: `/user/queue/dm-notification`
- **메시지 형태**:
```json
{
  "id": "uuid",
  "senderId": "123",
  "senderName": "김철수",
  "receiverId": "456",
  "receiverName": "이영희",
  "content": "메시지 내용",
  "type": "TEXT|FILE|SYSTEM",
  "timestamp": "2024-01-01T10:00:00"
}
```

### REST API

#### 1. DM 히스토리 조회
- **URL**: `GET /api/chat/dm/history`
- **Parameters**:
  - `userId1` (required): 사용자1 ID
  - `userId2` (required): 사용자2 ID
  - `page` (optional, default: 0): 페이지 번호
  - `size` (optional, default: 50): 페이지 크기
- **Response**:
```json
[
  {
    "id": 1,
    "senderId": 123,
    "receiverId": 456,
    "messageType": "DM",
    "content": "메시지 내용",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 2. 최근 DM 상대방 목록 조회
- **URL**: `GET /api/chat/dm/recent-partners`
- **Parameters**:
  - `userId` (required): 사용자 ID
- **Response**:
```json
[
  [456, "2024-01-01T10:00:00"],
  [789, "2024-01-01T09:30:00"]
]
```

## 📋 메시지 타입

### LectureChatMessage.MessageType
- `CHAT`: 일반 채팅
- `JOIN`: 입장 메시지
- `LEAVE`: 퇴장 메시지  
- `SYSTEM`: 시스템 메시지
- `ANNOUNCEMENT`: 공지사항 (강사/TA만 가능)

### DirectMessage.MessageType
- `TEXT`: 텍스트 메시지
- `FILE`: 파일 메시지
- `SYSTEM`: 시스템 메시지

### Message.MessageType (DB)
- `CHAT`: 일반 채팅
- `ANNOUNCEMENT`: 공지사항
- `SYSTEM`: 시스템 메시지
- `DM`: 다이렉트 메시지

## 🔗 WebSocket 구독 패턴

### 🏆 권장 구조 (Clean & Simple)
```javascript
// 필수 구독 2개만
1. `/room/lecture.{lectureId}`     // 강의실 모든 메시지 (히스토리+실시간)  
2. `/user/queue/notifications`     // 개인 알림 (DM, 멘션 등)

// 동적 구독 (필요시)
3. `/room/dm.{roomId}`            // DM 방 (열 때만 구독)
```

### 📱 메시지 타입으로 구분
```javascript
// 강의실 메시지 타입들
- HISTORY_SYNC     // 히스토리 동기화
- REALTIME_MESSAGE // 실시간 메시지  
- USER_JOIN        // 입장 알림
- USER_LEAVE       // 퇴장 알림
- ANNOUNCEMENT     // 공지사항

// 개인 알림 타입들  
- DM_RECEIVED      // DM 수신
- MENTION          // 멘션 알림
- ROOM_INVITE      // 방 초대
```

### 📋 현재 구조 (레거시)
<details>
<summary>기존 4개 구독 방식 (호환성)</summary>

1. **강의실 실시간**: `/topic/lecture.{lectureId}` 
2. **강의실 히스토리**: `/user/queue/lecture-history`
3. **DM 알림**: `/user/queue/dm-notification`
4. **DM 히스토리**: `/user/queue/dm-history`
5. **DM 실시간**: `/topic/dm.{roomId}` (동적)
</details>

## 🛠️ 완전한 구현 예시

### JavaScript 클라이언트 예시

#### 1. WebSocket 연결 및 구독 설정
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 메시지 순서 보장을 위한 큐잉 시스템
let messageQueue = [];
let historyLoading = false;

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});

function joinLecture() {
    const lectureId = '1';
    const userId = '123'; 
    const userName = '김철수';
    const userRole = 'STUDENT';
    
    // 필수 구독 4개 설정
    // 1. 강의실 실시간 메시지
    stompClient.subscribe(`/topic/lecture.${lectureId}`, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayLectureMessage(chatMessage);
    });
    
    // 2. DM 알림
    stompClient.subscribe('/user/queue/dm-notification', function(notification) {
        const data = JSON.parse(notification.body);
        showDMNotification(data);
    });
    
    // 3. DM 히스토리
    stompClient.subscribe('/user/queue/dm-history', function(historyMsg) {
        const history = JSON.parse(historyMsg.body);
        loadDMHistory(history);
    });
    
    // 4. 강의실 히스토리 (입장 직후 자동 수신)
    stompClient.subscribe('/user/queue/lecture-history', function(historyMsg) {
        const history = JSON.parse(historyMsg.body);
        loadLectureHistory(history);
    });
    
    // 강의실 입장 메시지 전송
    stompClient.send('/app/lecture.join', {}, JSON.stringify({
        lectureId: lectureId,
        userId: userId,
        userName: userName, 
        userRole: userRole
    }));
}
```

#### 2. 메시지 순서 보장 시스템
```javascript
// 실시간 메시지 처리 (큐잉 메커니즘)
function displayLectureMessage(message) {
    // 히스토리 로딩 중이면 큐에 저장
    if (historyLoading) {
        messageQueue.push(message);
        return;
    }
    // 즉시 표시
    displayMessageInternal(message);
}

// 히스토리 로드 (순서 보장)
function loadLectureHistory(history) {
    historyLoading = true; // 로딩 시작
    
    // 기존 메시지 클리어
    clearMessages();
    
    // 히스토리를 시간순으로 정렬 후 표시
    const sortedHistory = history.sort((a, b) => 
        new Date(a.createdAt) - new Date(b.createdAt)
    );
    
    sortedHistory.forEach(msg => {
        displayHistoryMessage(msg);
    });
    
    historyLoading = false; // 로딩 완료
    
    // 큐에 있던 실시간 메시지들 처리
    messageQueue.forEach(queuedMessage => {
        displayMessageInternal(queuedMessage);
    });
    messageQueue = []; // 큐 비우기
}
```

#### 3. 메시지 전송
```javascript
// 일반 채팅 메시지
function sendMessage(content) {
    stompClient.send('/app/lecture.send', {}, JSON.stringify({
        lectureId: currentLectureId,
        senderId: currentUserId,
        senderName: currentUserName,
        content: content,
        type: 'CHAT'
    }));
}

// 공지사항 (INSTRUCTOR/TA만)
function sendAnnouncement(content) {
    stompClient.send('/app/lecture.send', {}, JSON.stringify({
        lectureId: currentLectureId,
        senderId: currentUserId,
        senderName: currentUserName,
        content: content,
        type: 'ANNOUNCEMENT'
    }));
}
```

#### 4. DM 기능
```javascript
// DM 창 열기 (동적 구독)
function openDM(targetUserId, targetUserName) {
    const roomId = generateDMRoomId(currentUserId, targetUserId);
    
    // DM 방 구독
    stompClient.subscribe(`/topic/dm.${roomId}`, function(dmMessage) {
        const message = JSON.parse(dmMessage.body);
        displayDMMessage(message);
    });
    
    // DM 히스토리 요청
    stompClient.send('/app/dm.join', {}, JSON.stringify({
        userId1: currentUserId,
        userId2: targetUserId,
        requesterId: currentUserId
    }));
}

// DM 전송
function sendDM(content, receiverId, receiverName) {
    stompClient.send('/app/dm.send', {}, JSON.stringify({
        senderId: currentUserId,
        senderName: currentUserName,
        receiverId: receiverId,
        receiverName: receiverName,
        content: content
    }));
}
```

#### 5. 추가 페이징 (필요시)
```javascript
// 더 많은 히스토리 로드 (REST API 사용)
async function loadMoreHistory(page = 1) {
    try {
        const response = await fetch(
            `/api/chat/lecture/history?lectureId=${currentLectureId}&page=${page}&size=50`
        );
        const history = await response.json();
        prependHistoryMessages(history); // 위쪽에 추가
    } catch (error) {
        console.error('Failed to load more history:', error);
    }
}
```

## 📝 핵심 특징 및 주의사항

### ✨ 핵심 특징
1. **메시지 순서 보장**: 히스토리 로딩 중 실시간 메시지는 큐에 저장 후 순차 처리
2. **WebSocket 기반 히스토리**: 입장과 동시에 최근 50개 메시지 자동 전송 
3. **동적 구독**: DM 방은 필요할 때만 구독하여 효율성 확보
4. **실시간 알림**: DM 수신 시 즉시 알림 (창이 닫혀있어도)
5. **권한 기반 기능**: 공지사항은 강사/TA만 전송 가능

### ⚠️ 주의사항
1. **구독 순서**: 강의실 입장 전에 반드시 4개 필수 구독을 먼저 설정
2. **히스토리 메커니즘**: 
   - 기본 히스토리: WebSocket으로 자동 전송 (50개)
   - 추가 히스토리: REST API로 페이징 조회 
3. **메시지 순서**: `historyLoading` 플래그를 통한 큐잉 시스템 필수
4. **DM Room ID**: 사용자 ID를 정렬하여 생성 (`dm_{smaller}_{larger}`)
5. **사용자 인증**: 현재는 단순 ID 기반 (실운영시 JWT 등 인증 구현 필요)
6. **세션 관리**: WebSocket 연결 상태 관리 필수

### 🔄 실행 순서 (중요!)
```
1. WebSocket 연결
2. 4개 필수 구독 설정
3. 강의실 입장 메시지 전송  
4. 백엔드에서 히스토리 자동 전송
5. 히스토리 로딩 완료 후 실시간 메시지 처리 시작
```

## 🔧 기술 스택

- **Backend**: Spring Boot 3.x, Spring WebSocket, STOMP Protocol
- **Database**: MySQL 8.0 (메시지 저장)
- **Cache**: Redis (세션 관리, 참여자 관리)
- **Frontend**: SockJS, STOMP.js
- **Message Broker**: Spring Simple Broker
- **API**: WebSocket (주) + REST API (보조)