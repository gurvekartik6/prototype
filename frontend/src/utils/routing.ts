export function tierBonus(tier:number){return tier===1?.10:tier===2?.08:tier===3?.06:.04}
export function institutionScore(expertise:number,text:number,location:number,capacity:number,priority:number,tier:number){return Math.min(1,.50*expertise+.20*text+.12*location+.08*capacity+.05*priority+.05*tierBonus(tier))}
