import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const SignupForm = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    id: '',
    password: '',
    confirmPassword: '',
    school: '',
    grade: '',
    email: '',
    receiveEmails: false,
  });

  // 아이디 중복 확인 상태 관리
  const [idCheck, setIdCheck] = useState({
    checked: false, // 확인 완료 여부
    message: '',    // 상태 메시지 (사용 가능/불가능)
  });

  const [gradeOptions, setGradeOptions] = useState([]);

  useEffect(() => {
    if (form.school === '중등' || form.school === '고등') {
      setGradeOptions(['1학년', '2학년', '3학년']);
    } else if (form.school === '초등') {
      setGradeOptions(['1학년', '2학년', '3학년', '4학년', '5학년', '6학년']);
    } else {
      setGradeOptions([]);
    }
    setForm(prevForm => ({ ...prevForm, grade: '' }));
  }, [form.school]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
    
    // 아이디 필드를 수정하면, 중복 확인 상태를 초기화
    if (name === 'id') {
      setIdCheck({ checked: false, message: '' });
    }
  };

  // 아이디 중복 확인 함수
const handleIdCheck = async () => {
  if (!form.id) {
    alert('아이디를 먼저 입력해주세요.');
    return;
  }
  try {
    // 백엔드에 아이디 중복 확인 요청
    await axios.get(`http://localhost:8080/api/users/check-id?userid=${form.id}`);

    // [로직 변경 ①] 요청이 성공했다는 것은, 해당 유저가 존재한다는 의미입니다.
    // 따라서 '이미 사용 중인 아이디'로 처리해야 합니다.
    setIdCheck({ checked: false, message: '이미 사용 중인 아이디입니다.' });

  } catch (error) {
    // [로직 변경 ②] 요청이 실패했을 때, 어떤 에러인지 확인합니다.
    // error.response.status === 404는 'Not Found', 즉 유저가 없다는 의미입니다.
    if (error.response && error.response.status === 404) {
      // 이 경우가 진짜 '사용 가능한 아이디'입니다.
      setIdCheck({ checked: true, message: '사용 가능한 아이디입니다.' });
    } else {
      // 404 이외의 다른 서버 에러(500 등)나 네트워크 에러일 경우
      console.error("아이디 중복 확인 중 오류 발생:", error);
      setIdCheck({ checked: false, message: '오류가 발생했습니다. 다시 시도해주세요.' });
    }
  }
};

  const handleSubmit = async () => {
    // 유효성 검사
    if (!idCheck.checked) {
      alert('아이디 중복 확인을 해주세요.');
      return;
    }
    if (!form.name || !form.id || !form.password || !form.school || !form.grade || !form.email) {
      alert('모든 필수 항목을 입력해주세요.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.');
      return;
    }
    
    const schoolMapping = { '초등': 'ELEMENTARY', '중등': 'MIDDLE', '고등': 'HIGH' };
    const apiData = {
      userid: form.id,
      name: form.name,
      password: form.password,
      email: form.email,
      grade: parseInt(form.grade.replace('학년', '')),
      school: schoolMapping[form.school],
      receiveEmails: form.receiveEmails,
    };

    try {
      await axios.post('http://localhost:8080/api/users/signup', apiData);
      alert('회원가입이 성공적으로 완료되었습니다!');
      navigate('/', { state: { openLoginModal: true } });
    } catch (error) {
      if (error.response) {
        alert(`회원가입 실패: ${error.response.data.message || '서버에서 오류가 발생했습니다.'}`);
      } else {
        alert('회원가입 중 알 수 없는 오류가 발생했습니다.');
      }
    }
  };

  return (
    <div>
      <h2>회원가입</h2>
      <input name="name" placeholder="이름" onChange={handleChange} value={form.name} />
      
      <div className="input-with-button">
        <input name="id" placeholder="아이디" onChange={handleChange} value={form.id} />
        <button onClick={handleIdCheck} type="button">중복 확인</button>
      </div>
      {idCheck.message && <p className={`check-message ${idCheck.checked ? 'success' : 'error'}`}>{idCheck.message}</p>}

      <input name="password" type="password" placeholder="비밀번호" onChange={handleChange} value={form.password} />
      <input name="confirmPassword" type="password" placeholder="비밀번호 확인" onChange={handleChange} value={form.confirmPassword} />
      
      <div className="row">
        <select name="school" onChange={handleChange} value={form.school}>
          <option value="">학교</option>
          <option value="초등">초등</option>
          <option value="중등">중등</option>
          <option value="고등">고등</option>
        </select>
        <select name="grade" onChange={handleChange} value={form.grade}>
          <option value="">학년</option>
          {gradeOptions.map((grade, index) => (
            <option key={index} value={grade}>{grade}</option>
          ))}
        </select>
      </div>
      
      <input name="email" placeholder="이메일" onChange={handleChange} value={form.email} />
      
      <div className="checkbox">
        <input type="checkbox" name="receiveEmails" onChange={handleChange} checked={form.receiveEmails} />
        <label>복습 이메일 수신에 동의합니다.</label>
      </div>
      <button className="submit-btn" onClick={handleSubmit}>가입하기</button>
    </div>
  );
};

export default SignupForm;