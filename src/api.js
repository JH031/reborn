// src/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  // 프로젝트에서 쓰는 키 후보 모두 체크
  const token =
    localStorage.getItem('token') ||
    localStorage.getItem('accessToken') ||
    localStorage.getItem('jwt');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization; // 빈 Bearer null 방지
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      // 만료/미보관이면 정리
      ['token', 'accessToken', 'jwt'].forEach((k) => localStorage.removeItem(k));
    }
    return Promise.reject(err);
  }
);

export default api;
