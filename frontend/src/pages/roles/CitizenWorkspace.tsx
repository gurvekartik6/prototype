import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, Heart, MessageCircle, PlusCircle, Target, TrendingUp, Workflow } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Empty from '../../components/common/Empty';
import CommunityProblemCard from '../../components/problems/CommunityProblemCard';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../services/api';

export default function CitizenWorkspace() {
  const { user } = useAuth();
  const [problems, setProblems] = useState<any[]>([]);
  const [notifications, setNotifications] = useState<any[]>([]);
  const [points, setPoints] = useState<any[]>([]);
  const load = async () => {
    const [p, n, pts] = await Promise.allSettled([
      api.get('/problems'), api.get('/notifications'), api.get(`/users/${user.id}/points`),
    ]);
    if (p.status === 'fulfilled') setProblems(p.value.data || []);
    if (n.status === 'fulfilled') setNotifications(n.value.data || []);
    if (pts.status === 'fulfilled') setPoints(pts.value.data || []);
  };
  useEffect(() => { void load(); }, [user.id]);
  const mine = useMemo(() => problems.filter(p => String(p.userId) === user.id), [problems, user.id]);
  const active = mine.filter(p => !['COMPLETED','REJECTED','CANCELLED'].includes(p.status));
  const totalPoints = points.reduce((sum, x) => sum + Number(x.points || 0), 0);
  return <div className="space-y-6">
    <header className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
      <div><div className="text-xs uppercase tracking-widest font-black text-jharkhand-700">CITIZEN WORKSPACE</div><h1 className="text-3xl font-black mt-1">Your civic activity</h1><p className="text-slate-500 mt-1">Raise issues, strengthen community evidence and follow every step toward resolution.</p></div>
      <Link to="/submit" className="btn btn-primary"><PlusCircle size={17}/>Report a problem</Link>
    </header>
    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <StatCard label="My reports" value={mine.length} icon={Workflow}/><StatCard label="Active cases" value={active.length} icon={TrendingUp}/><StatCard label="Civic points" value={totalPoints} icon={Target}/><StatCard label="Unread updates" value={notifications.filter(n=>!n.read).length} icon={Bell}/>
    </div>
    <div className="grid lg:grid-cols-[1.25fr_.75fr] gap-5">
      <section className="card p-6"><div className="flex items-center justify-between"><div><h2 className="text-xl font-black">My reports</h2><p className="text-sm text-slate-500 mt-1">Track the status of every problem you raised.</p></div><Link to="/problems" className="text-sm font-bold text-jharkhand-700">Community feed →</Link></div>
        <div className="mt-4 space-y-4">{mine.length ? mine.slice(0,5).map(p=><CommunityProblemCard key={p.id} problem={p} compact/>) : <Empty/>}</div>
      </section>
      <aside className="space-y-5">
        <div className="card p-6"><h2 className="font-black">What you can do</h2><div className="mt-4 space-y-3">{[['Post','Raise a new civic problem.','/submit',PlusCircle],['Engage','Like, comment, repost and share community reports.','/',Heart],['Follow','Track validation, routing and resolution progress.','/problems',Workflow],['Give feedback','Add outcome feedback to completed work.','/notifications',MessageCircle]].map(([t,d,to,Icon]:any)=><Link to={to} key={t} className="flex gap-3 p-3 rounded-xl bg-slate-50 hover:bg-jharkhand-50"><div className="h-9 w-9 rounded-lg bg-white grid place-items-center text-jharkhand-700"><Icon size={17}/></div><div><div className="font-bold text-sm">{t}</div><div className="text-xs text-slate-500 mt-0.5">{d}</div></div></Link>)}</div></div>
        <div className="card p-6"><h2 className="font-black">Your contribution</h2><p className="text-sm text-slate-500 mt-2">Verified evidence and constructive participation help the civic team understand impact.</p><div className="mt-5 text-3xl font-black text-jharkhand-700">{totalPoints}<span className="text-sm text-slate-400 ml-2">points</span></div></div>
      </aside>
    </div>
  </div>;
}
