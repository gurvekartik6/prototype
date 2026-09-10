import axios from 'axios';
const configured=(import.meta.env.VITE_API_BASE_URL??'').trim().replace(/\/$/,'');
const baseURL=configured||(import.meta.env.DEV?'http://localhost:8080/api':'/api');
export const api=axios.create({baseURL,timeout:30000,headers:{Accept:'application/json'}});
api.interceptors.request.use(config=>{
  const token=localStorage.getItem('jh-token');
  const role=localStorage.getItem('jh-demo-role')||'USER';
  const userId=localStorage.getItem('jh-demo-user-id')||'user-demo';
  if(token) config.headers.Authorization=`Bearer ${token}`;
  config.headers['X-Demo-Role']=role;
  config.headers['X-Demo-User-Id']=userId;
  if(!(config.data instanceof FormData)) config.headers['Content-Type']='application/json';
  return config;
});
api.interceptors.response.use(r=>r,e=>{if(!e.response)e.message=`Backend is unreachable at ${baseURL}. Check that Spring Boot is running.`;return Promise.reject(e)});
