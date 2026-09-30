import { useEffect, useState } from "react";
import { StatusPill } from "./StatusPill";
import {
  IconBolt,
  IconChart,
  IconDrop,
  IconFan,
  IconHeadset,
  IconUser,
  IconWrench,
  KeystoneLogo,
} from "./Icons";

/** Main lifecycle path from the KEYSTONE spec (Section 07). */
const LIFECYCLE = ["OPEN", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CLOSED"] as const;

const ROLES = [
  {
    name: "Dispatcher",
    desc: "Raise work orders and assign the right technician.",
    Icon: IconHeadset,
  },
  {
    name: "Technician",
    desc: "Start, hold and complete jobs; log parts and time.",
    Icon: IconWrench,
  },
  {
    name: "Manager",
    desc: "Watch SLAs, overdue work and team load.",
    Icon: IconChart,
  },
  {
    name: "Customer",
    desc: "Raise requests and track them without a phone call.",
    Icon: IconUser,
  },
];

const TICKETS = [
  { id: "WO-1042", title: "Rooftop HVAC compressor tripping", site: "Meridian Tower B · Level 14", tech: "AK" },
  { id: "WO-1043", title: "Distribution board breaker fault", site: "Harbour Point · Basement 2", tech: "RS" },
  { id: "WO-1044", title: "Leak at chilled-water riser valve", site: "Westgate Plaza · Plant room", tech: "MV" },
];

function formatClock(totalSeconds: number) {
  const s = Math.max(0, totalSeconds);
  const h = String(Math.floor(s / 3600)).padStart(2, "0");
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
  const sec = String(s % 60).padStart(2, "0");
  return `${h}:${m}:${sec}`;
}

export default function AuthShowcase() {
  const [step, setStep] = useState(0);
  const [slaSeconds, setSlaSeconds] = useState(3 * 3600 + 42 * 60 + 10);

  // Walk a demo ticket through the lifecycle, then move to the next ticket.
  useEffect(() => {
    const id = window.setInterval(() => {
      setStep((s) => (s + 1) % (LIFECYCLE.length * TICKETS.length));
    }, 2200);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    const id = window.setInterval(() => setSlaSeconds((s) => (s <= 0 ? 4 * 3600 : s - 1)), 1000);
    return () => window.clearInterval(id);
  }, []);

  const stage = step % LIFECYCLE.length;
  const ticket = TICKETS[Math.floor(step / LIFECYCLE.length)];
  const progress = stage / (LIFECYCLE.length - 1);

  return (
    <aside className="showcase" aria-label="About KEYSTONE">
      <div className="showcase__top">
        <div className="showcase__brand">
          <KeystoneLogo size={38} />
          <div>
            <div className="showcase__brand-name">KEYSTONE</div>
            <div className="showcase__brand-sub">Field Service Platform</div>
          </div>
        </div>
        <span className="showcase__live">
          <span className="pulse-dot" />
          Meridian Facilities
        </span>
      </div>

      <div className="showcase__hero">
        <h1>
          Every request, <em>tracked</em> to a closed, accounted-for job.
        </h1>
        <p>
          One system of record for Meridian's maintenance operation — dispatchers assign, technicians
          update from the field, managers watch SLAs, and customers see progress in real time.
        </p>
        <div className="showcase__trades">
          <span className="showcase__trade">
            <IconFan /> HVAC
          </span>
          <span className="showcase__trade">
            <IconBolt /> Electrical
          </span>
          <span className="showcase__trade">
            <IconDrop /> Plumbing
          </span>
        </div>
      </div>

      <div className="lifecycle">
        <div className="lifecycle__label">
          <span>Work-order lifecycle</span>
          <span>Guarded transitions · audited</span>
        </div>
        <div className="lifecycle__track" style={{ ["--progress" as string]: progress }}>
          <div className="lifecycle__line" />
          {LIFECYCLE.map((name, i) => (
            <div
              key={name}
              className={
                "lifecycle__step" +
                (i < stage ? " lifecycle__step--done" : "") +
                (i === stage ? " lifecycle__step--current" : "")
              }
            >
              <span className="lifecycle__node" />
              <span className="lifecycle__name">{name}</span>
            </div>
          ))}
        </div>
        <div className="lifecycle__hold">
          IN_PROGRESS <span>⇄</span> <b>ON_HOLD</b> <span>waiting on parts or access</span>
        </div>
      </div>

      <div className="showcase__row">
        <div className="ticket" key={ticket.id}>
          <div className="ticket__head">
            <span className="ticket__id">{ticket.id}</span>
            <StatusPill status={LIFECYCLE[stage]} />
          </div>
          <div className="ticket__title">{ticket.title}</div>
          <div className="ticket__meta">{ticket.site}</div>
          <div className="ticket__foot">
            <div>
              <div className="ticket__sla-label">SLA DUE IN</div>
              <div className="ticket__sla">{formatClock(slaSeconds)}</div>
            </div>
            <div className="ticket__tech">
              <span className="ticket__avatar">{ticket.tech}</span>
              {stage === 0 ? "Unassigned" : "On site"}
            </div>
          </div>
        </div>

        <div className="roles">
          {ROLES.map(({ name, desc, Icon }) => (
            <div className="role-row" key={name}>
              <span className="role-row__icon">
                <Icon />
              </span>
              <div>
                <div className="role-row__name">{name}</div>
                <div className="role-row__desc">{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="showcase__foot">
        <span>Stateless JWT auth</span>
        <span>Role-based access</span>
        <span>SLA monitoring</span>
        <span>Full audit trail</span>
      </div>
    </aside>
  );
}
