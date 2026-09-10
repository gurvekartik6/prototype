import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

type LocationMapProps = {
  latitude?: number | null;
  longitude?: number | null;
  label?: string;
};

export default function LocationMap({
  latitude,
  longitude,
  label,
}: LocationMapProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current || latitude == null || longitude == null) {
      return;
    }

    const map = L.map(ref.current, {
      scrollWheelZoom: false,
    }).setView([latitude, longitude], 12);

    L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        attribution: '© OpenStreetMap contributors',
      }
    ).addTo(map);

    L.marker([latitude, longitude])
      .addTo(map)
      .bindPopup(label || 'Problem location');

    return () => {
      map.remove();
    };
  }, [latitude, longitude, label]);

  if (latitude == null || longitude == null) {
    return (
      <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
        GPS coordinates were not provided.
      </div>
    );
  }

  return (
    <div
      ref={ref}
      className="h-64 rounded-xl overflow-hidden border border-slate-200"
    />
  );
}