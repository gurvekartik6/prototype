import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import type { Role, User } from '../types';

const demos: Record<Role, User> = {
  USER: { id:'user-demo', name:'Demo Citizen', email:'user@jharkhand.gov.demo', role:'USER', civicPoints:120 },
  ADMIN: { id:'admin-demo', name:'Platform Administrator', email:'admin@jharkhand.gov.demo', role:'ADMIN', civicPoints:0 },
  DEPARTMENT: { id:'dept-demo', name:'Department Officer', email:'department@jharkhand.gov.demo', role:'DEPARTMENT', departmentId:'dep-06', civicPoints:0 },
  INSTITUTION: { id:'inst-demo', name:'Institution Coordinator', email:'institution@jharkhand.gov.demo', role:'INSTITUTION', institutionId:'tier1-01', civicPoints:0 },
  INDUSTRY: { id:'industry-demo', name:'Industry Partner', email:'industry@jharkhand.gov.demo', role:'INDUSTRY', industryId:'ind-01', civicPoints:0 },
};

type C = { user: User; loading:boolean; login:(e:string,p:string)=>Promise<void>; register:(n:string,e:string,p:string,l:string)=>Promise<void>; logout:()=>void; switchRole:(role:Role)=>void };
const Ctx=createContext<C>({user:demos.USER,loading:false,login:async()=>{},register:async()=>{},logout:()=>{},switchRole:()=>{}});

export function AuthProvider({children}:{children:ReactNode}){
  const [user,setUser]=useState<User>(()=>{try{return JSON.parse(localStorage.getItem('jh-demo-user')||'null')||demos.USER}catch{return demos.USER}});
  const switchRole=(role:Role)=>{const next=demos[role];setUser(next);localStorage.setItem('jh-demo-user',JSON.stringify(next));localStorage.setItem('jh-demo-role',role);localStorage.setItem('jh-demo-user-id',next.id);localStorage.removeItem('jh-token');};
  const login=async(e:string)=>{const role=(Object.values(demos).find(x=>x.email===e)?.role)||'USER';switchRole(role)};
  const register=async()=>switchRole('USER');
  const logout=()=>switchRole('USER');
  const value=useMemo(()=>({user,loading:false,login,register,logout,switchRole}),[user]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
export const useAuth=()=>useContext(Ctx);
export { demos };
