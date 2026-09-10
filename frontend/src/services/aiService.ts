import{api}from'./api';export const aiService={analyze:(id:string)=>api.post(`/problems/${id}/analyze`),retry:(id:string)=>api.post(`/problems/${id}/analyze/retry`)};
