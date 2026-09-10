import { Navigate } from 'react-router-dom';import type {ReactNode} from 'react';import type {Role} from '../../types';import{useAuth}from'../../context/AuthContext';
const home:Record<Role,string>={USER:'/dashboard',ADMIN:'/admin',DEPARTMENT:'/department',INSTITUTION:'/institution',INDUSTRY:'/industry'};
export default function RoleGate({roles,children}:{roles:Role[];children:ReactNode}){const{user}=useAuth();return roles.includes(user.role)?<>{children}</>:<Navigate to={home[user.role]} replace/>}
