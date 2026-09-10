import{render,screen}from'@testing-library/react';import{describe,it,expect}from'vitest';import StatusTimeline from'./StatusTimeline';
describe('StatusTimeline',()=>{it('renders the current workflow stage',()=>{render(<StatusTimeline current="PENDING_VALIDATION"/>);expect(screen.getByText(/Pending validation/i)).toBeInTheDocument();});});
