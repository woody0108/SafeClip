import { ClipboardList, ShieldCheck, UserCheck } from "lucide-react";

const reviewSteps = [
  {
    icon: ClipboardList,
    title: "Submissions",
    text: "Firestore submissions list and detail review"
  },
  {
    icon: ShieldCheck,
    title: "Evidence",
    text: "Photo/video access will use Storage or a company upload server later"
  },
  {
    icon: UserCheck,
    title: "Status",
    text: "Reviewing, report package ready, completed, or rejected"
  }
];

export function App() {
  return (
    <main className="admin-shell">
      <aside className="sidebar">
        <div className="brand-mark">SC</div>
        <div>
          <h1>SafeClip Admin</h1>
          <p>Company review console</p>
        </div>
      </aside>

      <section className="workspace">
        <header className="page-header">
          <div>
            <p className="eyebrow">Admin Web MVP</p>
            <h2>Submission review workspace</h2>
          </div>
          <button type="button">Sign in later</button>
        </header>

        <section className="summary-grid">
          {reviewSteps.map((item) => {
            const Icon = item.icon;
            return (
              <article key={item.title} className="summary-card">
                <Icon size={24} />
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </article>
            );
          })}
        </section>

        <section className="panel">
          <h3>Next implementation order</h3>
          <ol>
            <li>Firebase admin sign-in</li>
            <li>Read Firestore submissions</li>
            <li>Open submission detail</li>
            <li>Update review status</li>
          </ol>
        </section>
      </section>
    </main>
  );
}

