import React from "react";
import DateUtil from "../DateUtil";
import SessionStep from "./SessionStep";

function Session(props) {
    return  <div>
        <h2> Tree: {props.session.details.treeId} [{props.session.details.version}],
            duration: {DateUtil.formatTime(props.session.details.duration)}<br/></h2>
        <h3>Steps: &nbsp;</h3>
        {
            props.session.steps.map((step, index) => {
                    const min = Date.parse(props.session.details.date)
                    const max = Date.parse(props.session.details.date) + props.session.details.duration
                    return <SessionStep key={index}
                                        min={min}
                                        max={max}
                                        step={step}
                                        index={index}
                    />
                }
            )}
    </div>;
}

export default Session