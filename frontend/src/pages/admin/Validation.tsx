import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BrainCircuit,
  CheckCircle2,
  Route,
  ShieldCheck,
  XCircle,
  Edit3,
  MessageCircle,
  RefreshCw,
  AlertCircle,
  Loader2,
} from 'lucide-react';

import { api } from '../../services/api';
import Badge from '../../components/common/Badge';
import Empty from '../../components/common/Empty';

type ValidationProblem = {
  id: string;
  title: string;
  description?: string;
  category?: string;
  status: string;
  isDemoData?: boolean;
  summary?: string;
  domain?: string;
  subDomain?: string;
  recommendedPriority?: string;
};

const VALIDATION_STATUSES = [
  'PENDING_VALIDATION',
  'VALIDATED',
  'ROUTED',
  'NEEDS_CLARIFICATION',
];

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export default function Validation() {
  const [rows, setRows] = useState<ValidationProblem[]>([]);
  const [edit, setEdit] = useState<ValidationProblem | null>(null);

  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const response = await api.get('/admin/problems');

      const data = Array.isArray(response.data) ? response.data : [];

      const filtered = data.filter((problem: ValidationProblem) =>
        VALIDATION_STATUSES.includes(problem.status),
      );

      setRows(filtered);
    } catch (err: any) {
      console.error('Failed to load validation queue:', err);

      setRows([]);

      setError(
        err?.response?.data?.message ||
          'Unable to load the validation queue. Please check that the backend is running.',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  /*
   * IMPORTANT:
   * Never write useEffect(load, []) when load is async.
   *
   * Calling load directly makes React receive a Promise as the
   * effect cleanup function, which causes:
   *
   * "useEffect must not return anything besides a function"
   * "destroy is not a function"
   *
   * The effect itself returns nothing.
   */
  useEffect(() => {
    void load();
  }, [load]);

  const review = async (
    problem: ValidationProblem,
    decision: string,
  ) => {
    setActionId(problem.id);
    setError('');

    try {
      const changes =
        edit?.id === problem.id
          ? {
              summary: edit.summary || '',
              domain: edit.domain || '',
              subDomain: edit.subDomain || '',
              recommendedPriority:
                edit.recommendedPriority || 'MEDIUM',
            }
          : undefined;

      await api.post(`/admin/problems/${problem.id}/ai-review`, {
        decision,
        comments:
          decision === 'APPROVE'
            ? 'Validated by admin reviewer'
            : decision === 'REQUEST_CLARIFICATION'
              ? 'Citizen clarification required'
              : 'Analysis rejected',
        changes,
      });

      setEdit(null);

      await load();
    } catch (err: any) {
      console.error('AI review failed:', err);

      setError(
        err?.response?.data?.message ||
          `Unable to ${decision.toLowerCase().replaceAll('_', ' ')} this analysis.`,
      );
    } finally {
      setActionId(null);
    }
  };

  const route = async (problem: ValidationProblem) => {
    setActionId(problem.id);
    setError('');

    try {
      await api.post(`/problems/${problem.id}/route`);

      await load();
    } catch (err: any) {
      console.error('Routing generation failed:', err);

      setError(
        err?.response?.data?.message ||
          'Routing could not be generated.',
      );
    } finally {
      setActionId(null);
    }
  };

  const approveRoute = async (problem: ValidationProblem) => {
    setActionId(problem.id);
    setError('');

    try {
      const response = await api.get(
        `/problems/${problem.id}/routing`,
      );

      const routing = response.data;

      if (!routing?.id) {
        throw new Error(
          'No routing record was found for this problem.',
        );
      }

      await api.post(`/routing/${routing.id}/approve`);

      await load();
    } catch (err: any) {
      console.error('Routing approval failed:', err);

      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Routing approval failed.',
      );
    } finally {
      setActionId(null);
    }
  };

  const startEdit = (problem: ValidationProblem) => {
    setError('');

    setEdit({
      ...problem,
      summary: problem.summary || '',
      domain: problem.domain || '',
      subDomain: problem.subDomain || '',
      recommendedPriority:
        problem.recommendedPriority || 'MEDIUM',
    });
  };

  const cancelEdit = () => {
    setEdit(null);
  };

  const isBusy = (id: string) => actionId === id;

  const statusTone = (
    status: string,
  ): 'amber' | 'red' | 'green' | 'slate' => {
    if (status === 'PENDING_VALIDATION') {
      return 'amber';
    }

    if (status === 'NEEDS_CLARIFICATION') {
      return 'red';
    }

    if (status === 'VALIDATED' || status === 'ROUTED') {
      return 'green';
    }

    return 'slate';
  };

  return (
    <div className="space-y-6">
      {/* PAGE HEADER */}
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <div className="text-xs uppercase tracking-widest font-bold text-jharkhand-700">
            Governance
          </div>

          <h1 className="text-3xl font-black mt-1">
            AI &amp; routing validation
          </h1>

          <p className="text-slate-500 mt-1 max-w-3xl">
            Review AI output, edit it when necessary, then explicitly
            generate and approve deterministic routing.
          </p>
        </div>

        <button
          type="button"
          onClick={() => void load()}
          disabled={loading}
          className="btn btn-secondary"
        >
          {loading ? (
            <Loader2 size={15} className="animate-spin" />
          ) : (
            <RefreshCw size={15} />
          )}
          Refresh
        </button>
      </div>

      {/* ERROR */}
      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 flex items-start gap-3">
          <AlertCircle
            size={19}
            className="text-red-600 mt-0.5 shrink-0"
          />

          <div className="flex-1">
            <div className="font-bold text-red-800">
              Validation action failed
            </div>

            <div className="text-sm text-red-700 mt-1">
              {error}
            </div>
          </div>

          <button
            type="button"
            onClick={() => setError('')}
            className="text-red-500 hover:text-red-700 font-bold"
          >
            ×
          </button>
        </div>
      )}

      {/* LOADING */}
      {loading ? (
        <div className="card p-10 flex flex-col items-center justify-center text-center">
          <Loader2
            size={30}
            className="animate-spin text-jharkhand-700"
          />

          <div className="font-bold mt-4">
            Loading validation queue
          </div>

          <div className="text-sm text-slate-500 mt-1">
            Fetching problems awaiting human governance review.
          </div>
        </div>
      ) : !rows.length ? (
        <Empty title="Validation queue is clear" />
      ) : (
        <div className="space-y-4">
          {rows.map((problem) => {
            const editing =
              edit?.id === problem.id ? edit : null;

            const busy = isBusy(problem.id);

            return (
              <div
                className="card p-6"
                key={problem.id}
              >
                <div className="flex flex-col lg:flex-row gap-5">
                  {/* PROBLEM INFORMATION */}
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap gap-2">
                      {problem.category && (
                        <Badge>{problem.category}</Badge>
                      )}

                      <Badge tone={statusTone(problem.status)}>
                        {problem.status.replaceAll('_', ' ')}
                      </Badge>

                      {problem.isDemoData && (
                        <Badge tone="slate">
                          DEMO DATA
                        </Badge>
                      )}
                    </div>

                    <Link
                      to={`/problems/${problem.id}`}
                      className="font-black text-xl mt-3 block hover:text-jharkhand-700 transition-colors"
                    >
                      {problem.title}
                    </Link>

                    <p className="text-sm text-slate-500 mt-2 line-clamp-3">
                      {problem.description ||
                        'No problem description available.'}
                    </p>

                    {/* EDIT FORM */}
                    {editing && (
                      <div className="mt-5 rounded-2xl border border-jharkhand-100 bg-jharkhand-50/40 p-4">
                        <div className="flex items-center justify-between gap-3 mb-3">
                          <div>
                            <div className="font-black">
                              Admin override
                            </div>

                            <div className="text-xs text-slate-500 mt-1">
                              Changes will be recorded as part of
                              the human review.
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={cancelEdit}
                            className="text-sm font-bold text-slate-500 hover:text-slate-800"
                          >
                            Cancel
                          </button>
                        </div>

                        <div className="grid md:grid-cols-2 gap-3">
                          <div className="md:col-span-2">
                            <label className="text-xs font-bold text-slate-600">
                              Approved summary
                            </label>

                            <textarea
                              className="input mt-1 min-h-24"
                              value={editing.summary || ''}
                              onChange={(event) =>
                                setEdit({
                                  ...editing,
                                  summary: event.target.value,
                                })
                              }
                              placeholder="Approved problem summary"
                            />
                          </div>

                          <div>
                            <label className="text-xs font-bold text-slate-600">
                              Domain
                            </label>

                            <input
                              className="input mt-1"
                              value={editing.domain || ''}
                              onChange={(event) =>
                                setEdit({
                                  ...editing,
                                  domain: event.target.value,
                                })
                              }
                              placeholder="Domain"
                            />
                          </div>

                          <div>
                            <label className="text-xs font-bold text-slate-600">
                              Sub-domain
                            </label>

                            <input
                              className="input mt-1"
                              value={editing.subDomain || ''}
                              onChange={(event) =>
                                setEdit({
                                  ...editing,
                                  subDomain: event.target.value,
                                })
                              }
                              placeholder="Sub-domain"
                            />
                          </div>

                          <div>
                            <label className="text-xs font-bold text-slate-600">
                              Recommended priority
                            </label>

                            <select
                              className="input mt-1"
                              value={
                                editing.recommendedPriority ||
                                'MEDIUM'
                              }
                              onChange={(event) =>
                                setEdit({
                                  ...editing,
                                  recommendedPriority:
                                    event.target.value,
                                })
                              }
                            >
                              {PRIORITIES.map((priority) => (
                                <option
                                  key={priority}
                                  value={priority}
                                >
                                  {priority}
                                </option>
                              ))}
                            </select>
                          </div>
                        </div>

                        <div className="flex flex-wrap gap-2 mt-4">
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                              void review(
                                problem,
                                'APPROVE',
                              )
                            }
                            className="btn btn-primary"
                          >
                            {busy ? (
                              <Loader2
                                size={14}
                                className="animate-spin"
                              />
                            ) : (
                              <CheckCircle2 size={14} />
                            )}
                            Save &amp; approve
                          </button>

                          <button
                            type="button"
                            disabled={busy}
                            onClick={cancelEdit}
                            className="btn btn-secondary"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* AI REVIEW PANEL */}
                  <div className="lg:w-96 bg-slate-50 rounded-2xl p-4 border border-slate-100">
                    <div className="flex items-center gap-2 font-bold">
                      <BrainCircuit
                        size={17}
                        className="text-blue-600"
                      />

                      AI review
                    </div>

                    <div className="text-xs text-slate-500 mt-2">
                      AI remains advisory. Admin approval is required
                      before routing.
                    </div>

                    {/* CURRENT AI INFORMATION */}
                    <div className="grid grid-cols-2 gap-2 mt-4">
                      <div className="rounded-xl bg-white border border-slate-100 p-3">
                        <div className="text-[10px] uppercase tracking-wide text-slate-400 font-bold">
                          Category
                        </div>

                        <div className="text-sm font-bold mt-1">
                          {problem.category || '—'}
                        </div>
                      </div>

                      <div className="rounded-xl bg-white border border-slate-100 p-3">
                        <div className="text-[10px] uppercase tracking-wide text-slate-400 font-bold">
                          Priority
                        </div>

                        <div className="text-sm font-bold mt-1">
                          {problem.recommendedPriority ||
                            'MEDIUM'}
                        </div>
                      </div>
                    </div>

                    {/* REVIEW ACTIONS */}
                    <div className="grid grid-cols-2 gap-2 mt-4">
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => startEdit(problem)}
                        className="btn btn-secondary"
                      >
                        <Edit3 size={14} />
                        Edit
                      </button>

                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void review(
                            problem,
                            'REQUEST_CLARIFICATION',
                          )
                        }
                        className="btn btn-secondary"
                      >
                        {busy ? (
                          <Loader2
                            size={14}
                            className="animate-spin"
                          />
                        ) : (
                          <MessageCircle size={14} />
                        )}
                        Clarify
                      </button>

                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void review(
                            problem,
                            'APPROVE',
                          )
                        }
                        className="btn btn-primary"
                      >
                        {busy ? (
                          <Loader2
                            size={14}
                            className="animate-spin"
                          />
                        ) : (
                          <CheckCircle2 size={14} />
                        )}
                        Approve
                      </button>

                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void review(
                            problem,
                            'REJECT',
                          )
                        }
                        className="btn btn-danger"
                      >
                        {busy ? (
                          <Loader2
                            size={14}
                            className="animate-spin"
                          />
                        ) : (
                          <XCircle size={14} />
                        )}
                        Reject
                      </button>
                    </div>

                    {/* ROUTING GENERATION */}
                    {problem.status === 'VALIDATED' && (
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => void route(problem)}
                        className="btn btn-secondary w-full mt-2"
                      >
                        {busy ? (
                          <Loader2
                            size={14}
                            className="animate-spin"
                          />
                        ) : (
                          <Route size={14} />
                        )}

                        Generate routing
                      </button>
                    )}

                    {/* ROUTING APPROVAL */}
                    {problem.status === 'ROUTED' && (
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          void approveRoute(problem)
                        }
                        className="btn btn-primary w-full mt-2"
                      >
                        {busy ? (
                          <Loader2
                            size={14}
                            className="animate-spin"
                          />
                        ) : (
                          <ShieldCheck size={14} />
                        )}

                        Approve routing
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ROUTING POLICY */}
      <div className="card p-6">
        <div className="flex items-start gap-3">
          <Route className="text-jharkhand-700 mt-0.5 shrink-0" />

          <div>
            <div className="font-black">
              Routing rule
            </div>

            <div className="text-sm text-slate-500 mt-1">
              Initial routing is only to Government Department
              and/or Higher Education Institution. Industry enters
              later during the collaboration and deployment stages.
            </div>
          </div>

          <ShieldCheck className="ml-auto text-jharkhand-600 shrink-0" />
        </div>
      </div>
    </div>
  );
}
