import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

// ---------------- Helpers ----------------
const OPTION_LABELS = {
  FIND_MY_ERROR: '내 오류 찾기',
  APPROACH: '접근 방식',
  FULL_SOLUTION: '전체 풀이',
};

const parseAiResponse = (displayData) => {
  if (!displayData) return '답변을 분석했습니다.';
  const {
    problem_summary, asked, correct_answer,
    step_by_step_explain, feedback, error_analysis,
    needed_concepts, how_to_approach
  } = displayData;

  const parts = [];
  if (problem_summary) parts.push(`📌 문제 요약\n${problem_summary}`);
  if (asked) parts.push(`❓ 질문\n${asked}`);
  if (needed_concepts) parts.push(`🧠 필요한 개념\n${needed_concepts.join(', ')}`);
  if (how_to_approach) parts.push(`🔍 접근 방식\n${how_to_approach}`);
  if (error_analysis) {
    const errorExplanation = error_analysis.map(err => err.explanation).join('\n');
    parts.push(`🧐 오류 분석\n${errorExplanation}`);
  }
  if (step_by_step_explain) parts.push(`📝 단계별 풀이\n${step_by_step_explain}`);
  if (correct_answer) parts.push(`✅ 정답\n${correct_answer}`);
  if (feedback) parts.push(`💡 피드백\n${feedback}`);
  return parts.join('\n\n').trim() || '답변 정보가 없습니다.';
};

// ```json ``` 코드블록 제거 후 JSON 파싱 시도
const cleanAndParseJson = (content) => {
  if (!content || typeof content !== 'string') return null;
  const cleaned = content.trim().replace(/^```json\s*|```\s*$/g, '');
  try {
    const parsed = JSON.parse(cleaned);
    if (parsed && typeof parsed === 'object') return parsed;
  } catch (_) {}
  return null;
};

// 시스템 액션(유사문제 버튼 등) 텍스트 패턴
const isSystemActionUserText = (txt) => {
  if (!txt) return false;
  const t = String(txt).trim();

  return /^(유사\s*문제|유사문제)\s*(\d+\s*개\s*)?(생성|요청|만들어|주세요|보기|봐줘)/.test(t);
};

// 모델 응답 정규화
const normalizeModelContent = (content, turn) => {
  if (content && typeof content === 'object' && !Array.isArray(content)) {
    const { responseType, display, similarProblems, message } = content;
    if (responseType === 'ANALYSIS' && display) {
      return { type: 'analysis', text: parseAiResponse(display), isActionable: turn === 1 };
    }
    if (responseType === 'SIMILAR_PROBLEMS' && Array.isArray(similarProblems)) {
      return { type: 'similar', problems: similarProblems };
    }
    if (responseType === 'TEXT' && typeof message === 'string') {
      return { type: 'text', text: message };
    }
    return { type: 'text', text: JSON.stringify(content, null, 2) };
  }
  if (typeof content === 'string') {
    const parsed = cleanAndParseJson(content);
    if (parsed && typeof parsed === 'object') return normalizeModelContent(parsed, turn);
    return { type: 'text', text: content };
  }
  return { type: 'text', text: '' };
};

// ---------------- Provider ----------------
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [problemId, setProblemId] = useState(null);
  const [messages, setMessages] = useState([
    { id: 'initial-welcome', text: '안녕하세요! REBORN에 오신 것을 환영합니다.', sender: 'ai' }
  ]);

  useEffect(() => {
    try {
      const storedProfile = localStorage.getItem('userProfile');
      if (storedProfile) {
        const { user: storedUser, token: storedToken } = JSON.parse(storedProfile);
        if (storedUser && storedUser.id) {
          setUser(storedUser);
          setToken(storedToken);
          setMessages([
            { id: `welcome-${storedUser.id}`, text: '안녕하세요! REBORN에 오신 것을 환영합니다.', sender: 'ai' }
          ]);
        }
      }
    } catch (error) {
      console.error('저장된 프로필 파싱 실패:', error);
      localStorage.removeItem('userProfile');
    }
  }, []);

  const login = (loginData) => {
    const { token, id, userid, name, email, grade, school } = loginData;
    const userToStore = { id, userid, name, email, grade, school };
    localStorage.setItem('userProfile', JSON.stringify({ user: userToStore, token }));
    setUser(userToStore);
    setToken(token);
    setMessages([{ id: `welcome-${id}`, text: '안녕하세요! REBORN에 오신 것을 환영합니다.', sender: 'ai' }]);
  };

  const logout = () => {
    localStorage.removeItem('userProfile');
    setUser(null);
    setToken(null);
    startNewChat();
  };

  const startNewChat = () => {
    setMessages([{ id: 'initial-welcome', text: '안녕하세요! REBORN에 오신 것을 환영합니다.', sender: 'ai' }]);
    setProblemId(null);
  };

  // 모델 응답 공통 처리
  const processAiResponse = (result) => {
    if (result.similarProblems) {
      return { type: 'similar', problems: result.similarProblems, text: null };
    }
    if (result.display) {
      return { type: 'analysis', text: parseAiResponse(result.display), isActionable: !problemId };
    }
    if (result.message) {
      return { type: 'text', text: result.message };
    }
    return { type: 'text', text: typeof result === 'object' ? JSON.stringify(result, null, 2) : '알 수 없는 형식의 답변입니다.' };
  };

  // ---------------- Actions ----------------
  const handleSendMessage = async (userRequest, file, option) => {
    if (!userRequest && !file) return;

    const optionLabel = option ? `[${OPTION_LABELS[option]}]` : '';
    const displayTextForUser = file ? `${optionLabel}${userRequest ? `\n${userRequest}` : ''}` : userRequest;

    const userMessage = {
      id: Date.now(),
      text: displayTextForUser,
      file: file ? URL.createObjectURL(file) : null,
      sender: 'user',
    };

    const loadingMessage = { id: Date.now() + 1, text: '답변을 생성하고 있어요...', sender: 'ai' };
    setMessages(prev => [...prev, userMessage, loadingMessage]);

    try {
      let response;
      const userId = user?.id;
      if (!userId || !token) {
        alert('로그인이 필요합니다.');
        setMessages(prev => prev.filter(msg => msg.id !== loadingMessage.id));
        return;
      }

      if (problemId) {
        const formData = new FormData();
        formData.append('prompt', userRequest);
        if (file) formData.append('image', file);
        const url = `http://localhost:8080/api/problems/${problemId}/ask?userId=${userId}&prompt=${encodeURIComponent(userRequest)}`;
        response = await fetch(url, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: formData });
      } else {
        const formData = new FormData();
        if (file) formData.append('image', file);
        formData.append('userRequest', userRequest);
        formData.append('option', option);
        formData.append('userId', userId);
        response = await fetch('http://localhost:8080/api/problems/analyze-first', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          body: formData,
        });
      }

      if (!response.ok) throw new Error(`API 요청 실패: ${response.status}`);
      const result = await response.json();

      if (!problemId && result.problemId) {
        setProblemId(result.problemId);
        try {
          const firstUserText = displayTextForUser || '[문제 업로드]';
          const firstUserImage = result.imageUrl || (userMessage.file || null); 
          const meta = { firstUserText, firstUserImage, savedAt: Date.now() };
          localStorage.setItem(`chatMeta:${result.problemId}`, JSON.stringify(meta));
        } catch (_) {}
      }

      const processed = processAiResponse(result);
      setMessages(prev => prev.map(msg => (msg.id === loadingMessage.id ? { ...msg, ...processed } : msg)));
    } catch (error) {
      console.error('API Error:', error);
      setMessages(prev => prev.map(msg => (msg.id === loadingMessage.id ? { ...msg, text: '오류가 발생했습니다. 잠시 후 다시 시도해주세요.' } : msg)));
    }
  };

  const handleRequestSimilarProblems = async () => {
    if (!problemId || !user) return;

    const loadingMessage = { id: Date.now(), text: '유사 문제를 생성하고 있어요...', sender: 'ai' };
    setMessages(prev => [...prev, loadingMessage]);

    try {
      const response = await fetch(`http://localhost:8080/api/problems/${problemId}/similar?userId=${user.id}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('유사 문제 요청 실패');

      const result = await response.json();
      const sims =
        Array.isArray(result?.similarProblems) ? result.similarProblems :
        Array.isArray(result?.problems)        ? result.problems :
        Array.isArray(result)                  ? result : [];

      setMessages(prev => prev.map(msg => (msg.id === loadingMessage.id ? { ...msg, type: 'similar', problems: sims, text: null } : msg)));
    } catch (error) {
      console.error(error);
      setMessages(prev => prev.map(msg => (msg.id === loadingMessage.id ? { ...msg, type: 'text', text: '유사 문제 생성에 실패했습니다.' } : msg)));
    }
  };

  // ---------------- History Loader ----------------
  const loadChatHistory = async (selectedProblemId) => {
    if (!user || !token) {
      alert('로그인이 필요합니다.');
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/api/problems/${selectedProblemId}/history?userId=${user.id}`,
        { headers: { 'Authorization': `Bearer ${token}` } }
      );
      if (!response.ok) throw new Error('대화 기록을 불러오는데 실패했습니다.');

      const data = await response.json();
      const chatHistory = Array.isArray(data.chatTurns) ? data.chatTurns : [];

      const transformed = chatHistory.flatMap(chat => {
        const items = [];

        // (1) 사용자 말풍선 (시스템 액션 숨김)
        if (chat.role === 'user') {
          const uText = typeof chat.content === 'string' ? chat.content : JSON.stringify(chat.content, null, 2);
          if (!isSystemActionUserText(uText)) {
            items.push({
              id: `${chat.turn}-user-${chat.createdAt}`,
              turn: chat.turn,
              text: uText,
              sender: 'user',
              file: chat.imageUrl || null,
              type: 'text',
            });
          }
        }

        // (2) 모델 응답
        if (chat.role === 'model') {
          const normalized = normalizeModelContent(chat.content, chat.turn);
          items.push({
            id: `${chat.turn}-model-${chat.createdAt}`,
            turn: chat.turn,
            text: normalized.text || null,
            sender: 'ai',
            type: normalized.type,
            isActionable: normalized.isActionable || false,
            problems: normalized.problems || null,
          });
        }

        return items;
      });

      const deduped = [];
      for (const m of transformed) {
        const last = deduped[deduped.length - 1];
        if (
          last &&
          last.sender === m.sender &&
          typeof last.text === 'string' &&
          typeof m.text === 'string' &&
          last.text.trim() === m.text.trim()
        ) continue;
        deduped.push(m);
      }

      const hasFirstUserTurn = deduped.some(m => m.sender === 'user' && m.turn === 1);
      let finalMessages = deduped;
      if (!hasFirstUserTurn) {
        try {
          const raw = localStorage.getItem(`chatMeta:${data.problemId}`);
          if (raw) {
            const meta = JSON.parse(raw);
            if (meta && (meta.firstUserText || meta.firstUserImage)) {
              finalMessages = [
                {
                  id: `problem-${data.problemId}-user-first`,
                  turn: 1,
                  sender: 'user',
                  text: meta.firstUserText || '[문제 업로드]',
                  file: meta.firstUserImage || data.originalImageUrl || null,
                  type: 'text',
                },
                ...deduped,
              ];
            }
          }
        } catch (_) {}
      }

      setMessages(finalMessages);
      setProblemId(data.problemId);
    } catch (error) {
      console.error(error);
      alert(error.message);
    }
  };

  const value = {
    user,
    token,
    login,
    logout,
    messages,
    problemId,
    handleSendMessage,
    handleRequestSimilarProblems,
    loadChatHistory,
    startNewChat,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
