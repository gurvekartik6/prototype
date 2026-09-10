import{api}from'./api';export const routingService={route:(id:string)=>api.post(`/problems/${id}/route`),approve:(id:string)=>api.post(`/routing/${id}/approve`)};
