import { useState, type ReactNode } from "react";
import * as Popover from "@radix-ui/react-popover";
import * as Dialog from "@radix-ui/react-dialog";
import { ChevronDown, X } from "lucide-react";
import { useMediaQuery } from "../lib/use-media-query";
import { cn } from "../lib/utils";

export function ResponsivePicker({
  title,
  label,
  children,
  className,
}: {
  title: string;
  label: ReactNode;
  children: (close: () => void) => ReactNode;
  className?: string;
}) {
  const mobile = useMediaQuery("(max-width: 767px)");
  const [open, setOpen] = useState(false);
  const trigger = (
    <button
      type="button"
      className={cn(
        "flex min-h-12 w-full items-center justify-between gap-3 rounded-lg border border-slate-300 bg-white px-3 text-left text-sm text-slate-800 outline-none hover:bg-slate-50 focus-visible:ring-2 focus-visible:ring-brand-600",
        className,
      )}
    >
      <span className="min-w-0 flex-1">{label}</span>
      <ChevronDown size={18} className="shrink-0 text-slate-400" />
    </button>
  );
  const content = children(() => setOpen(false));
  if (mobile) {
    return (
      <Dialog.Root open={open} onOpenChange={setOpen}>
        <Dialog.Trigger asChild>{trigger}</Dialog.Trigger>
        <Dialog.Portal>
          <Dialog.Overlay className="fixed inset-0 z-[60] bg-slate-950/35" />
          <Dialog.Content className="fixed inset-x-0 bottom-0 z-[60] max-h-[85dvh] overflow-y-auto rounded-t-2xl bg-white px-4 pb-[calc(1rem+env(safe-area-inset-bottom))] pt-4 shadow-2xl">
            <div className="mb-4 flex items-center justify-between">
              <Dialog.Title className="text-lg font-semibold text-slate-950">
                {title}
              </Dialog.Title>
              <Dialog.Close
                className="grid size-11 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
                aria-label="Закрыть"
              >
                <X size={21} />
              </Dialog.Close>
            </div>
            {content}
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    );
  }
  return (
    <Popover.Root open={open} onOpenChange={setOpen}>
      <Popover.Trigger asChild>{trigger}</Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={8}
          className="z-[60] max-h-[min(28rem,var(--radix-popover-content-available-height))] w-[var(--radix-popover-trigger-width)] min-w-72 overflow-y-auto rounded-xl border border-slate-200 bg-white p-2 shadow-xl"
        >
          <p className="px-2 pb-2 pt-1 text-sm font-semibold text-slate-900">
            {title}
          </p>
          {content}
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}
