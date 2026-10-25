const LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'Started',
  DONE: 'Done',
  FAILED: 'Not done',
  PARTIAL: 'Partially done',
};

export default function SessionStatus({ session }) {
  return (
    <>
      <p><strong>{LABELS[session.status] || 'Open'}</strong></p>
      {session.carried && <p>Carried over from a completely unmarked previous week.</p>}
    </>
  );
}