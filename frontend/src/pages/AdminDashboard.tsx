import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { ServiceItem, AppointmentItem, AppointmentSlotItem, ApiErrorResponse } from '../types';
import { 
  Shield, 
  Layers, 
  Clock, 
  Calendar, 
  Plus, 
  CheckCircle, 
  XCircle, 
  AlertCircle, 
  RefreshCw
} from 'lucide-react';
import { AxiosError } from 'axios';

export const AdminDashboard: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'appointments' | 'services' | 'slots'>('appointments');

  // Appointments state
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [appointmentsLoading, setAppointmentsLoading] = useState(false);
  const [apptStatusFilter, setApptStatusFilter] = useState<string>('ALL');

  // Services state
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [serviceName, setServiceName] = useState('');
  const [serviceDesc, setServiceDesc] = useState('');
  const [serviceDuration, setServiceDuration] = useState('30');
  const [serviceCreating, setServiceCreating] = useState(false);
  const [serviceSuccess, setServiceSuccess] = useState<string | null>(null);
  const [serviceError, setServiceError] = useState<string | null>(null);

  // Slots state
  const [slots, setSlots] = useState<AppointmentSlotItem[]>([]);
  const [slotServiceId, setSlotServiceId] = useState('');
  const [slotStartTime, setSlotStartTime] = useState('');
  const [slotEndTime, setSlotEndTime] = useState('');
  const [slotCreating, setSlotCreating] = useState(false);
  const [slotSuccess, setSlotSuccess] = useState<string | null>(null);
  const [slotError, setSlotError] = useState<string | null>(null);

  const fetchAdminAppointments = async () => {
    setAppointmentsLoading(true);
    try {
      const params: Record<string, string> = {};
      if (apptStatusFilter !== 'ALL') {
        params.status = apptStatusFilter;
      }
      const res = await api.get<AppointmentItem[]>('/admin/appointments', { params });
      setAppointments(res.data);
    } catch (e) {
      console.error('Failed to load admin appointments', e);
    } finally {
      setAppointmentsLoading(false);
    }
  };

  const fetchServices = async () => {
    try {
      const res = await api.get<ServiceItem[]>('/services');
      setServices(res.data);
      if (res.data.length > 0 && !slotServiceId) {
        setSlotServiceId(res.data[0].id.toString());
      }
    } catch (e) {
      console.error('Failed to load services', e);
    }
  };

  const fetchSlots = async () => {
    try {
      const res = await api.get<AppointmentSlotItem[]>('/slots');
      setSlots(res.data);
    } catch (e) {
      console.error('Failed to load slots', e);
    }
  };

  useEffect(() => {
    fetchAdminAppointments();
    fetchServices();
    fetchSlots();
  }, [apptStatusFilter]);

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    setServiceCreating(true);
    setServiceError(null);
    setServiceSuccess(null);

    const duration = parseInt(serviceDuration, 10);
    if (!serviceName.trim()) {
      setServiceError('Service name is required');
      setServiceCreating(false);
      return;
    }
    if (isNaN(duration) || duration <= 0) {
      setServiceError('Duration must be a positive number of minutes');
      setServiceCreating(false);
      return;
    }

    try {
      await api.post<ServiceItem>('/services', {
        name: serviceName.trim(),
        description: serviceDesc.trim() || undefined,
        durationMinutes: duration,
        active: true,
      });

      setServiceSuccess('Service created successfully!');
      setServiceName('');
      setServiceDesc('');
      setServiceDuration('30');
      fetchServices();
      setTimeout(() => setServiceSuccess(null), 3000);
    } catch (err) {
      const error = err as AxiosError<ApiErrorResponse>;
      setServiceError(error.response?.data?.message || 'Failed to create service.');
    } finally {
      setServiceCreating(false);
    }
  };

  const handleCreateSlot = async (e: React.FormEvent) => {
    e.preventDefault();
    setSlotCreating(true);
    setSlotError(null);
    setSlotSuccess(null);

    if (!slotServiceId) {
      setSlotError('Please choose a service for this slot');
      setSlotCreating(false);
      return;
    }
    if (!slotStartTime || !slotEndTime) {
      setSlotError('Both start and end time are required');
      setSlotCreating(false);
      return;
    }

    try {
      await api.post<AppointmentSlotItem>('/slots', {
        serviceId: parseInt(slotServiceId, 10),
        startTime: slotStartTime,
        endTime: slotEndTime,
        active: true,
      });

      setSlotSuccess('Appointment slot created successfully!');
      setSlotStartTime('');
      setSlotEndTime('');
      fetchSlots();
      setTimeout(() => setSlotSuccess(null), 3000);
    } catch (err) {
      const error = err as AxiosError<ApiErrorResponse>;
      setSlotError(error.response?.data?.message || 'Failed to create slot.');
    } finally {
      setSlotCreating(false);
    }
  };

  const formatDateTime = (isoString: string) => {
    const d = new Date(isoString);
    return {
      date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-5">
        <div>
          <div className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-800 mb-1.5 border border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800">
            <Shield className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />
            <span>ROLE_ADMIN Authorization</span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Administrator Management Portal</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Manage system services, create bookable slots, and monitor all organization appointments.
          </p>
        </div>

        {/* Tab switcher */}
        <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1 shadow-sm self-start dark:border-slate-800 dark:bg-slate-900">
          <button
            onClick={() => setActiveTab('appointments')}
            className={`flex items-center gap-1.5 rounded-md px-3.5 py-2 text-xs font-semibold transition-colors ${
              activeTab === 'appointments' ? 'bg-brand-600 text-white dark:bg-brand-500' : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            <Calendar className="h-4 w-4" />
            All Bookings ({appointments.length})
          </button>
          <button
            onClick={() => setActiveTab('services')}
            className={`flex items-center gap-1.5 rounded-md px-3.5 py-2 text-xs font-semibold transition-colors ${
              activeTab === 'services' ? 'bg-brand-600 text-white dark:bg-brand-500' : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            <Layers className="h-4 w-4" />
            Manage Services ({services.length})
          </button>
          <button
            onClick={() => setActiveTab('slots')}
            className={`flex items-center gap-1.5 rounded-md px-3.5 py-2 text-xs font-semibold transition-colors ${
              activeTab === 'slots' ? 'bg-brand-600 text-white dark:bg-brand-500' : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            <Clock className="h-4 w-4" />
            Manage Slots ({slots.length})
          </button>
        </div>
      </div>

      {/* TAB 1: ALL APPOINTMENTS */}
      {activeTab === 'appointments' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Filter by status:</span>
              <div className="inline-flex rounded-lg border border-slate-200 bg-white p-0.5 text-xs dark:border-slate-800 dark:bg-slate-900">
                {(['ALL', 'BOOKED', 'CANCELLED'] as const).map((status) => (
                  <button
                    key={status}
                    onClick={() => setApptStatusFilter(status)}
                    className={`rounded px-2.5 py-1 font-semibold ${
                      apptStatusFilter === status ? 'bg-slate-900 text-white dark:bg-slate-700' : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
                    }`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={fetchAdminAppointments}
              className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${appointmentsLoading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                <thead className="bg-slate-50 text-xs font-semibold text-slate-700 uppercase border-b border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-750">
                  <tr>
                    <th className="py-3 px-4">Ref #</th>
                    <th className="py-3 px-4">Patient / User</th>
                    <th className="py-3 px-4">Service</th>
                    <th className="py-3 px-4">Date & Time</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Reason / Notes</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {appointmentsLoading ? (
                    <tr>
                      <td colSpan={6} className="py-8 text-center text-slate-400 dark:text-slate-500">
                        Loading appointments...
                      </td>
                    </tr>
                  ) : appointments.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-8 text-center text-slate-400 dark:text-slate-500">
                        No appointments found matching current filter.
                      </td>
                    </tr>
                  ) : (
                    appointments.map((appt) => {
                      const start = formatDateTime(appt.startTime);
                      const isBooked = appt.status === 'BOOKED';

                      return (
                        <tr key={appt.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/50">
                          <td className="py-3.5 px-4 font-mono text-xs font-semibold text-slate-900 dark:text-white">
                            #{appt.id}
                          </td>
                          <td className="py-3.5 px-4">
                            <div className="font-semibold text-slate-900 dark:text-white">{appt.userFullName}</div>
                            <div className="text-xs text-slate-400 dark:text-slate-500">{appt.userEmail}</div>
                          </td>
                          <td className="py-3.5 px-4 font-medium text-slate-800 dark:text-slate-200">
                            {appt.serviceName}
                          </td>
                          <td className="py-3.5 px-4 text-xs">
                            <div className="font-medium text-slate-900 dark:text-white">{start.date}</div>
                            <div className="text-slate-500 dark:text-slate-400">{start.time}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <span
                              className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${
                                isBooked ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300' : 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-400'
                              }`}
                            >
                              {isBooked ? <CheckCircle className="h-3 w-3" /> : <XCircle className="h-3 w-3" />}
                              {appt.status}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-xs text-slate-500 dark:text-slate-400 max-w-xs truncate">
                            {appt.reason || '—'}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: MANAGE SERVICES */}
      {activeTab === 'services' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Create Service Form */}
          <div className="lg:col-span-1 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
              <Plus className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              Add New Service
            </h2>

            {serviceSuccess && (
              <div className="mb-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-xs text-emerald-800 border border-emerald-200 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
                <CheckCircle className="h-4 w-4 text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
                <span>{serviceSuccess}</span>
              </div>
            )}

            {serviceError && (
              <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 p-3 text-xs text-red-800 border border-red-200 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
                <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0" />
                <span>{serviceError}</span>
              </div>
            )}

            <form onSubmit={handleCreateService} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Service Name
                </label>
                <input
                  type="text"
                  required
                  value={serviceName}
                  onChange={(e) => setServiceName(e.target.value)}
                  placeholder="e.g. Dermatology Consultation"
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Duration (Minutes)
                </label>
                <input
                  type="number"
                  required
                  min="5"
                  max="480"
                  value={serviceDuration}
                  onChange={(e) => setServiceDuration(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={serviceDesc}
                  onChange={(e) => setServiceDesc(e.target.value)}
                  placeholder="Clinical diagnostic check, skin assessment..."
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <button
                type="submit"
                disabled={serviceCreating}
                className="w-full rounded-lg bg-brand-600 py-2 px-4 text-sm font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 disabled:opacity-60"
              >
                {serviceCreating ? 'Creating Service...' : 'Create Service'}
              </button>
            </form>
          </div>

          {/* Current Services List */}
          <div className="lg:col-span-2 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-base font-bold text-slate-900 dark:text-white mb-4">Existing Active Services</h2>
            <div className="space-y-3">
              {services.map((s) => (
                <div key={s.id} className="flex items-center justify-between p-3.5 rounded-lg border border-slate-100 bg-slate-50/50 dark:border-slate-800 dark:bg-slate-800/40">
                  <div>
                    <h3 className="font-semibold text-slate-900 dark:text-white">{s.name}</h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{s.description || 'No description provided'}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-semibold bg-brand-50 text-brand-700 px-2.5 py-1 rounded-md dark:bg-brand-950/60 dark:text-brand-300">
                      {s.durationMinutes} mins
                    </span>
                    <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">Active</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: MANAGE SLOTS */}
      {activeTab === 'slots' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Create Slot Form */}
          <div className="lg:col-span-1 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
              <Plus className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              Schedule New Slot
            </h2>

            {slotSuccess && (
              <div className="mb-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-3 text-xs text-emerald-800 border border-emerald-200 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
                <CheckCircle className="h-4 w-4 text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
                <span>{slotSuccess}</span>
              </div>
            )}

            {slotError && (
              <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 p-3 text-xs text-red-800 border border-red-200 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
                <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0" />
                <span>{slotError}</span>
              </div>
            )}

            <form onSubmit={handleCreateSlot} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Service
                </label>
                <select
                  required
                  value={slotServiceId}
                  onChange={(e) => setSlotServiceId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                >
                  <option value="">Select Service...</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id.toString()}>
                      {s.name} ({s.durationMinutes}m)
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Start Date & Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={slotStartTime}
                  onChange={(e) => setSlotStartTime(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  End Date & Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={slotEndTime}
                  onChange={(e) => setSlotEndTime(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <button
                type="submit"
                disabled={slotCreating}
                className="w-full rounded-lg bg-brand-600 py-2 px-4 text-sm font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 disabled:opacity-60"
              >
                {slotCreating ? 'Publishing Slot...' : 'Publish Slot'}
              </button>
            </form>
          </div>

          {/* Current Slots List */}
          <div className="lg:col-span-2 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="text-base font-bold text-slate-900 dark:text-white mb-4">Bookable Slots Currently Scheduled</h2>
            <div className="max-h-96 overflow-y-auto space-y-2">
              {slots.length === 0 ? (
                <p className="text-xs text-slate-400 dark:text-slate-500 text-center py-6">No slots scheduled yet.</p>
              ) : (
                slots.map((slot) => {
                  const s = formatDateTime(slot.startTime);
                  const e = formatDateTime(slot.endTime);
                  return (
                    <div key={slot.id} className="flex items-center justify-between p-3 rounded-lg border border-slate-100 bg-slate-50/50 dark:border-slate-800 dark:bg-slate-800/40 text-sm">
                      <div>
                        <span className="font-semibold text-slate-900 dark:text-white">{slot.serviceName}</span>
                        <div className="text-xs text-slate-500 dark:text-slate-400">{s.date} • {s.time} - {e.time}</div>
                      </div>
                      <span className="text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 px-2 py-0.5 rounded">
                        Available
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
