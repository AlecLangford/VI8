package inc.flide.vi8.keyboardActionListners;

import android.os.Handler;
import android.view.HapticFeedbackConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import inc.flide.vi8.MainInputMethodService;
import inc.flide.vi8.structures.Constants;
import inc.flide.vi8.structures.FingerPosition;
import inc.flide.vi8.keyboardHelpers.KeyboardAction;
import inc.flide.vi8.views.mainKeyboard.XboardView;

public class MainKeyboardActionListener {

    private MainInputMethodService mainInputMethodService;
    private XboardView mainKeyboardView;

    private Map<List<FingerPosition>, KeyboardAction> keyboardActionMap;

    private List<FingerPosition> movementSequence;
    private FingerPosition currentFingerPosition;
    private boolean isLongPressCallbackSet;

    public MainKeyboardActionListener(MainInputMethodService inputMethodService,
                                      XboardView view) {
        this.mainInputMethodService = inputMethodService;
        this.mainKeyboardView = view;

        keyboardActionMap = mainInputMethodService.buildKeyboardActionMap();

        movementSequence = new ArrayList<>();
        currentFingerPosition = FingerPosition.NO_TOUCH;
    }

    public void movementStarted(FingerPosition fingerPosition) {
        currentFingerPosition = fingerPosition;
        movementSequence.clear();
        movementSequence.add(currentFingerPosition);
        initiateLongPressDetection();
    }

    public void movementContinues(FingerPosition fingerPosition) {
        FingerPosition lastKnownFingerPosition = currentFingerPosition;
        currentFingerPosition = fingerPosition;

        boolean isFingerPositionChanged = (lastKnownFingerPosition != currentFingerPosition);

        if(isFingerPositionChanged){
            interruptLongPress();
            movementSequence.add(currentFingerPosition);
            if(currentFingerPosition == FingerPosition.INSIDE_CIRCLE
                    && keyboardActionMap.get(movementSequence)!=null){
                processMovementSequence(movementSequence);
                movementSequence.clear();
                movementSequence.add(currentFingerPosition);
            }
        }else if(!isLongPressCallbackSet){
            initiateLongPressDetection();
        }
    }

    public void movementEnds() {
        interruptLongPress();
        currentFingerPosition = FingerPosition.NO_TOUCH;
        movementSequence.add(currentFingerPosition);
        processMovementSequence(movementSequence);
        movementSequence.clear();
    }

    private final Handler longPressHandler = new Handler();
    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            List<FingerPosition> movementSequenceAgumented = new ArrayList<>(movementSequence);
            movementSequenceAgumented.add(FingerPosition.LONG_PRESS);
            processMovementSequence(movementSequenceAgumented);
            longPressHandler.postDelayed(this, Constants.DELAY_MILLIS_LONG_PRESS_CONTINUATION);
        }
    };

    private void initiateLongPressDetection(){
        isLongPressCallbackSet = true;
        longPressHandler.postDelayed(longPressRunnable, Constants.DELAY_MILLIS_LONG_PRESS_INITIATION);
    }

    private void interruptLongPress(){
        longPressHandler.removeCallbacks(longPressRunnable);
        List<FingerPosition> movementSequenceAgumented = new ArrayList<>(movementSequence);
        movementSequenceAgumented.add(FingerPosition.LONG_PRESS_END);
        processMovementSequence(movementSequenceAgumented);
        isLongPressCallbackSet = false;
    }

    private void processMovementSequence(List<FingerPosition> movementSequence) {

        KeyboardAction keyboardAction = keyboardActionMap.get(movementSequence);

        boolean isMovementValid = true;
        if(keyboardAction == null){
            //Logger.Verbose(this, "No Action Mapping has been defined for the given Sequence : " + movementSequence.toString());
            movementSequence.clear();
            return;
        }

        switch (keyboardAction.getKeyboardActionType()){
            case INPUT_TEXT:
                mainInputMethodService.handleInputText(keyboardAction);
                break;
            case INPUT_KEY:
                mainInputMethodService.handleInputKey(keyboardAction);
                break;
            case INPUT_SPECIAL:
                mainInputMethodService.handleSpecialInput(keyboardAction);
                break;
            default:
                //Logger.Warn(this, "Action Type Undefined : " + keyboardAction.getKeyboardActionType().toString());
                isMovementValid = false;
        }
        if(isMovementValid){
            mainKeyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }
}
