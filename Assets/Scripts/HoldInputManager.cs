using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.XR.ARFoundation;

public class HoldInputManager : MonoBehaviour, IPointerDownHandler, IPointerUpHandler
{
    [SerializeField] private RotateFood rotateFoodScript;
    [SerializeField] private bool isRotatingRight;
    private bool isPressed = false;

    public void OnPointerDown(PointerEventData eventData)
    {
        isPressed = true;
        if (isRotatingRight)
        {
            rotateFoodScript.StartRotating(1);
        }
        else if (!isRotatingRight)
        {
            rotateFoodScript.StartRotating(-1);
        }
    }

    public void OnPointerUp(PointerEventData eventData)
    {
        isPressed = false;
        rotateFoodScript.StartRotating(0);
    }
}
