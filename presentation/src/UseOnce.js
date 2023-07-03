import {useEffect, useRef} from "react";

export const useOnce = (func) => {
    const initialized = useRef(false);

    useEffect(() => {
        if (initialized.current === true) return;
        initialized.current = true;

        func();

        return () => {
            initialized.current = false;
        }
    }, []);
}