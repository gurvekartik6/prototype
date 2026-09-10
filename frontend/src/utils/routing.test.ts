import{describe,it,expect}from'vitest';import{institutionScore,tierBonus}from'./routing';
describe('routing scoring',()=>{it('limits tier influence',()=>{expect(tierBonus(1)).toBe(.10);expect(tierBonus(4)).toBe(.04);});it('rewards expertise more than tier',()=>{const strong=institutionScore(1,1,0,1,.8,4);const weak=institutionScore(0,0,0,1,.8,1);expect(strong).toBeGreaterThan(weak);});});
